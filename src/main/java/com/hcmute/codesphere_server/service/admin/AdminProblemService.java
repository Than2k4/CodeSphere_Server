package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.enums.ContestType;
import com.hcmute.codesphere_server.model.payload.request.CreateProblemRequest;
import com.hcmute.codesphere_server.model.payload.response.ProblemDetailResponse;
import com.hcmute.codesphere_server.repository.common.*;
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

    @Transactional
    public ProblemDetailResponse createProblem(CreateProblemRequest request, Long authorId) {
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

        // Map sang response
        return mapToProblemDetailResponse(problem);
    }

    @Transactional
    public ProblemDetailResponse updateProblem(Long problemId, CreateProblemRequest request) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

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

        return mapToProblemDetailResponse(problem);
    }

    public ProblemDetailResponse getProblemById(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));
        return mapToProblemDetailResponse(problem);
    }

    @Transactional
    public void deleteProblem(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

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

