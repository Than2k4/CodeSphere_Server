package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.enums.ContestType;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateProblemRequest;
import com.hcmute.codesphere_server.model.payload.response.ProblemDetailResponse;
import com.hcmute.codesphere_server.repository.common.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProblemService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final ContestProblemRepository contestProblemRepository;
        private final AuditLogService auditLogService;
        private final ObjectMapper objectMapper;

    @Transactional
        public ProblemDetailResponse createProblem(
            CreateProblemRequest request,
            Long authorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        // Kiểm tra code đã tồn tại chưa
        if (problemRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Problem với code '" + request.getCode() + "' đã tồn tại");
        }

        // Lấy author
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Validate và lấy categories
        Set<CategoryEntity> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                CategoryEntity category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category với ID " + categoryId + " không tồn tại"));
                categories.add(category);
            }
        }

        // Validate và lấy languages
        Set<LanguageEntity> languages = new HashSet<>();
        if (request.getLanguageIds() != null && !request.getLanguageIds().isEmpty()) {
            for (Long languageId : request.getLanguageIds()) {
                LanguageEntity language = languageRepository.findById(languageId)
                        .orElseThrow(() -> new RuntimeException("Language với ID " + languageId + " không tồn tại"));
                languages.add(language);
            }
        }

        // Validate level
        String level = request.getLevel().toUpperCase();
        if (!level.equals("EASY") && !level.equals("MEDIUM") && !level.equals("HARD")) {
            throw new RuntimeException("Level phải là EASY, MEDIUM hoặc HARD");
        }

        // Tạo problem mới
        Instant now = Instant.now();
        ProblemEntity problem = ProblemEntity.builder()
                .code(request.getCode().toUpperCase())
                .title(request.getTitle())
                .content(request.getContent())
                .level(level)
                .timeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 2000)
                .memoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256)
                .author(author)
                .status(true)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .createdAt(now)
                .updatedAt(now)
                .categories(categories)
                .languages(languages)
                .build();

        problem = problemRepository.save(problem);

        recordAudit(
            authorId,
            actorUsername,
            actorRole,
            "PROBLEM_CREATE",
            problem.getId(),
            problem.getCode() + " - " + problem.getTitle(),
            null,
            toJson(snapshotProblem(problem)),
            "Create problem",
            ipAddress,
            userAgent
        );

        // Map sang response
        return mapToProblemDetailResponse(problem);
    }

    @Transactional
        public ProblemDetailResponse updateProblem(
            Long problemId,
            CreateProblemRequest request,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

        String beforeState = toJson(snapshotProblem(problem));

        // Kiểm tra code đã tồn tại chưa (nếu thay đổi)
        if (!problem.getCode().equals(request.getCode()) && problemRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Problem với code '" + request.getCode() + "' đã tồn tại");
        }

        // Validate và lấy categories
        Set<CategoryEntity> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                CategoryEntity category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category với ID " + categoryId + " không tồn tại"));
                categories.add(category);
            }
        }

        // Validate và lấy languages
        Set<LanguageEntity> languages = new HashSet<>();
        if (request.getLanguageIds() != null && !request.getLanguageIds().isEmpty()) {
            for (Long languageId : request.getLanguageIds()) {
                LanguageEntity language = languageRepository.findById(languageId)
                        .orElseThrow(() -> new RuntimeException("Language với ID " + languageId + " không tồn tại"));
                languages.add(language);
            }
        }

        // Validate level
        String level = request.getLevel().toUpperCase();
        if (!level.equals("EASY") && !level.equals("MEDIUM") && !level.equals("HARD")) {
            throw new RuntimeException("Level phải là EASY, MEDIUM hoặc HARD");
        }

        // Kiểm tra nếu đang thay đổi từ contest-only (isPublic = false) sang public (isPublic = true)
        boolean changingToPublic = request.getIsPublic() != null && request.getIsPublic();
        boolean wasContestOnly = Boolean.FALSE.equals(problem.getIsPublic());
        
        if (changingToPublic && wasContestOnly) {
            // Kiểm tra problem có đang trong OFFICIAL contest chưa kết thúc không
            List<ContestProblemEntity> contestProblems = contestProblemRepository.findByProblemId(problemId);
            List<String> ongoingOfficialContests = new ArrayList<>();
            List<String> upcomingOfficialContests = new ArrayList<>();
            Instant now = Instant.now();
            
            for (ContestProblemEntity cp : contestProblems) {
                ContestEntity contest = cp.getContest();
                
                // CHỈ KIỂM TRA OFFICIAL CONTEST (bỏ qua PRACTICE)
                if (contest.getContestType() == ContestType.OFFICIAL) {
                    // Kiểm tra contest chưa kết thúc
                    if (contest.getEndTime() != null && now.isBefore(contest.getEndTime())) {
                        if (contest.getStartTime() != null && now.isBefore(contest.getStartTime())) {
                            // Contest chưa bắt đầu
                            upcomingOfficialContests.add(contest.getTitle());
                        } else {
                            // Contest đang diễn ra
                            ongoingOfficialContests.add(contest.getTitle());
                        }
                    }
                }
                // Bỏ qua PRACTICE contest - không cần validation
            }
            
            if (!ongoingOfficialContests.isEmpty()) {
                throw new RuntimeException(
                    "Cannot change problem to public. It is currently used in ongoing OFFICIAL contest(s): " + 
                    String.join(", ", ongoingOfficialContests) + 
                    ". Please wait until contests end or remove it from contests first."
                );
            }
            
            if (!upcomingOfficialContests.isEmpty()) {
                throw new RuntimeException(
                    "Cannot change problem to public. It is scheduled for upcoming OFFICIAL contest(s): " + 
                    String.join(", ", upcomingOfficialContests) + 
                    ". Please remove it from contests first or wait until contests end."
                );
            }
        }

        // Cập nhật problem
        problem.setCode(request.getCode().toUpperCase());
        problem.setTitle(request.getTitle());
        problem.setContent(request.getContent());
        problem.setLevel(level);
        problem.setTimeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 2000);
        problem.setMemoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256);
        problem.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        problem.setUpdatedAt(Instant.now());
        problem.setCategories(categories);
        problem.setLanguages(languages);

        problem = problemRepository.save(problem);

        recordAudit(
            actorId,
            actorUsername,
            actorRole,
            "PROBLEM_UPDATE",
            problem.getId(),
            problem.getCode() + " - " + problem.getTitle(),
            beforeState,
            toJson(snapshotProblem(problem)),
            "Update problem",
            ipAddress,
            userAgent
        );

        return mapToProblemDetailResponse(problem);
    }

    public ProblemDetailResponse getProblemById(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));
        return mapToProblemDetailResponse(problem);
    }

    @Transactional
    public void deleteProblem(
            Long problemId,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

        String beforeState = toJson(snapshotProblem(problem));

        // Kiểm tra problem có đang được dùng trong contest không
        List<ContestProblemEntity> contestProblems = contestProblemRepository.findByProblemId(problemId);
        if (!contestProblems.isEmpty()) {
            List<String> contestNames = contestProblems.stream()
                    .map(cp -> cp.getContest().getTitle())
                    .distinct()
                    .collect(Collectors.toList());
            
            throw new RuntimeException(
                "Cannot delete problem. It is currently used in " + contestProblems.size() + 
                " contest(s): " + String.join(", ", contestNames) + 
                ". Please remove it from contests first."
            );
        }

        // Xóa problem
        problemRepository.delete(problem);

        recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "PROBLEM_DELETE",
                problemId,
                problem.getCode() + " - " + problem.getTitle(),
                beforeState,
                null,
                "Delete problem",
                ipAddress,
                userAgent
        );
    }

    private java.util.Map<String, Object> snapshotProblem(ProblemEntity problem) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", problem.getId());
        snapshot.put("code", problem.getCode());
        snapshot.put("title", problem.getTitle());
        snapshot.put("level", problem.getLevel());
        snapshot.put("timeLimitMs", problem.getTimeLimitMs());
        snapshot.put("memoryLimitMb", problem.getMemoryLimitMb());
        snapshot.put("status", problem.getStatus());
        snapshot.put("isPublic", problem.getIsPublic());
        snapshot.put("categoryIds", problem.getCategories() == null ? java.util.List.of() : problem.getCategories().stream().map(CategoryEntity::getId).sorted().collect(Collectors.toList()));
        snapshot.put("languageIds", problem.getLanguages() == null ? java.util.List.of() : problem.getLanguages().stream().map(LanguageEntity::getId).sorted().collect(Collectors.toList()));
        return snapshot;
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void recordAudit(
            Long actorId,
            String actorUsername,
            String actorRole,
            String action,
            Long objectId,
            String objectLabel,
            String beforeState,
            String afterState,
            String changeSummary,
            String ipAddress,
            String userAgent
    ) {
        try {
            auditLogService.record(
                    AuditLogRecordRequest.builder()
                            .actorId(actorId)
                            .actorUsername(actorUsername)
                            .actorRole(actorRole)
                            .action(action)
                            .objectType("PROBLEM")
                            .objectId(objectId)
                            .objectLabel(objectLabel)
                            .beforeState(beforeState)
                            .afterState(afterState)
                            .changeSummary(changeSummary)
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .build()
            );
        } catch (Exception ignored) {
            // Keep problem action successful even if audit log fails
        }
    }

    private ProblemDetailResponse mapToProblemDetailResponse(ProblemEntity entity) {
        return ProblemDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .content(entity.getContent())
                .level(entity.getLevel())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getUsername() : null)
                .isPublic(entity.getIsPublic())
                .categories(entity.getCategories().stream()
                        .map(cat -> com.hcmute.codesphere_server.model.payload.response.CategoryResponse.builder()
                                .id(cat.getId())
                                .name(cat.getName())
                                .slug(cat.getSlug())
                                .parentId(null)
                                .parentName(null)
                                .build())
                        .collect(Collectors.toList()))
                .languages(entity.getLanguages().stream()
                        .map(lang -> com.hcmute.codesphere_server.model.payload.response.LanguageResponse.builder()
                                .id(lang.getId())
                                .code(lang.getCode())
                                .name(lang.getName())
                                .version(lang.getVersion())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

