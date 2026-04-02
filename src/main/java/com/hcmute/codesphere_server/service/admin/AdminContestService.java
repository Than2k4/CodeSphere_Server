package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.entity.embedded.ContestProblemKey;
import com.hcmute.codesphere_server.model.enums.ContestType;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.request.ContestProblemRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateContestRequest;
import com.hcmute.codesphere_server.model.payload.response.ContestDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.ContestProblemResponse;
import com.hcmute.codesphere_server.model.payload.response.ContestResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemResponse;
import com.hcmute.codesphere_server.repository.common.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
        private final AuditLogService auditLogService;
        private final ObjectMapper objectMapper;

    @Transactional
        public ContestDetailResponse createContest(
            CreateContestRequest request,
            Long authorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        // Validate contestType
        if (request.getContestType() == null) {
            throw new RuntimeException("Contest type không được để trống");
        }

        // Validate based on contest type
        if (request.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: durationMinutes required, startTime/endTime should be null
            if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
                throw new RuntimeException("PRACTICE contest phải có durationMinutes > 0");
            }
            if (request.getStartTime() != null || request.getEndTime() != null) {
                throw new RuntimeException("PRACTICE contest không được có startTime/endTime");
            }
        } else if (request.getContestType() == ContestType.OFFICIAL) {
            // OFFICIAL: startTime/endTime required, durationMinutes should be null
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new RuntimeException("OFFICIAL contest phải có startTime và endTime");
            }
            if (request.getEndTime().isBefore(request.getStartTime())) {
                throw new RuntimeException("End time phải sau start time");
            }
            if (request.getDurationMinutes() != null) {
                throw new RuntimeException("OFFICIAL contest không được có durationMinutes");
            }
        }

        // Validate access code for private contests
        if (!request.getIsPublic() && (request.getAccessCode() == null || request.getAccessCode().trim().isEmpty())) {
            throw new RuntimeException("Private contest phải có access code");
        }

        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Create contest
        ContestEntity contest = ContestEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .contestType(request.getContestType())
                .durationMinutes(request.getDurationMinutes())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .registrationStartTime(request.getRegistrationStartTime())
                .registrationEndTime(request.getRegistrationEndTime())
                .isPublic(request.getIsPublic())
                .accessCode(request.getAccessCode())
                .author(author)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isDeleted(false)
                .build();

        contest = contestRepository.save(contest);

        // Add problems if provided
        if (request.getProblems() != null && !request.getProblems().isEmpty()) {
            for (ContestProblemRequest cpReq : request.getProblems()) {
                ProblemEntity problem = problemRepository.findById(cpReq.getProblemId())
                        .orElseThrow(() -> new RuntimeException("Problem với ID " + cpReq.getProblemId() + " không tồn tại"));

                // Validate problem: phải active (status = true)
                if (!Boolean.TRUE.equals(problem.getStatus())) {
                    throw new RuntimeException("Problem với ID " + cpReq.getProblemId() + " không active (status = false)");
                }

                ContestProblemKey key = new ContestProblemKey();
                key.setContestId(contest.getId());
                key.setProblemId(problem.getId());

                ContestProblemEntity contestProblem = ContestProblemEntity.builder()
                        .id(key)
                        .contest(contest)
                        .problem(problem)
                        .problemOrder(cpReq.getOrder().toUpperCase())
                        .points(cpReq.getPoints() != null ? cpReq.getPoints() : 100)
                        .build();

                contestProblemRepository.save(contestProblem);
                
                // Tự động set isPublic = false để problem không hiện ở ProblemsPage (contest-only)
                if (Boolean.TRUE.equals(problem.getIsPublic())) {
                    problem.setIsPublic(false);
                    problemRepository.save(problem);
                }
            }
        }

        recordAudit(
                authorId,
                actorUsername,
                actorRole,
                "CONTEST_CREATE",
                contest.getId(),
                contest.getTitle(),
                null,
                toJson(snapshotContest(contest)),
                "Create contest",
                ipAddress,
                userAgent
        );

        return mapToContestDetailResponse(contest, null);
    }

    public ContestDetailResponse getContestById(Long contestId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        return mapToContestDetailResponse(contest, null);
    }

    @Transactional
        public ContestDetailResponse updateContest(
            Long contestId,
            CreateContestRequest request,
            Long authorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

            String beforeState = toJson(snapshotContest(contest));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // Validate contestType
        if (request.getContestType() == null) {
            throw new RuntimeException("Contest type không được để trống");
        }

        // Validate based on contest type
        if (request.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: durationMinutes required, startTime/endTime should be null
            if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
                throw new RuntimeException("PRACTICE contest phải có durationMinutes > 0");
            }
            if (request.getStartTime() != null || request.getEndTime() != null) {
                throw new RuntimeException("PRACTICE contest không được có startTime/endTime");
            }
        } else if (request.getContestType() == ContestType.OFFICIAL) {
            // OFFICIAL: startTime/endTime required, durationMinutes should be null
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new RuntimeException("OFFICIAL contest phải có startTime và endTime");
            }
            if (request.getEndTime().isBefore(request.getStartTime())) {
                throw new RuntimeException("End time phải sau start time");
            }
            if (request.getDurationMinutes() != null) {
                throw new RuntimeException("OFFICIAL contest không được có durationMinutes");
            }
        }

        // Update contest
        contest.setTitle(request.getTitle());
        contest.setDescription(request.getDescription());
        contest.setContestType(request.getContestType());
        contest.setDurationMinutes(request.getDurationMinutes());
        
        // Set startTime/endTime based on contest type
        if (request.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: set to null
            contest.setStartTime(null);
            contest.setEndTime(null);
        } else {
            // OFFICIAL: set from request
            contest.setStartTime(request.getStartTime());
            contest.setEndTime(request.getEndTime());
        }
        
        // Registration times are optional, set to null if not provided
        contest.setRegistrationStartTime(request.getRegistrationStartTime());
        contest.setRegistrationEndTime(request.getRegistrationEndTime());
        contest.setIsPublic(request.getIsPublic());
        contest.setAccessCode(request.getAccessCode());
        contest.setUpdatedAt(Instant.now());

        contest = contestRepository.save(contest);

        // Update problems: Xóa tất cả problems cũ và thêm lại từ request
        if (request.getProblems() != null) {
            // Xóa tất cả problems hiện tại của contest
            List<ContestProblemEntity> existingProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contestId);
            for (ContestProblemEntity cp : existingProblems) {
                contestProblemRepository.delete(cp);
            }

            // Thêm lại problems mới từ request
            if (!request.getProblems().isEmpty()) {
                for (ContestProblemRequest cpReq : request.getProblems()) {
                    ProblemEntity problem = problemRepository.findById(cpReq.getProblemId())
                            .orElseThrow(() -> new RuntimeException("Problem với ID " + cpReq.getProblemId() + " không tồn tại"));

                    // Validate problem: phải active (status = true)
                    if (!Boolean.TRUE.equals(problem.getStatus())) {
                        throw new RuntimeException("Problem với ID " + cpReq.getProblemId() + " không active (status = false)");
                    }

                    ContestProblemKey key = new ContestProblemKey();
                    key.setContestId(contest.getId());
                    key.setProblemId(problem.getId());

                    ContestProblemEntity contestProblem = ContestProblemEntity.builder()
                            .id(key)
                            .contest(contest)
                            .problem(problem)
                            .problemOrder(cpReq.getOrder().toUpperCase())
                            .points(cpReq.getPoints() != null ? cpReq.getPoints() : 100)
                            .build();

                    contestProblemRepository.save(contestProblem);
                    
                    // Tự động set isPublic = false để problem không hiện ở ProblemsPage (contest-only)
                    if (Boolean.TRUE.equals(problem.getIsPublic())) {
                        problem.setIsPublic(false);
                        problemRepository.save(problem);
                    }
                }
            }
        }

        recordAudit(
                authorId,
                actorUsername,
                actorRole,
                "CONTEST_UPDATE",
                contest.getId(),
                contest.getTitle(),
                beforeState,
                toJson(snapshotContest(contest)),
                "Update contest",
                ipAddress,
                userAgent
        );

        return mapToContestDetailResponse(contest, null);
    }

    @Transactional
        public void deleteContest(
            Long contestId,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

            String beforeState = toJson(snapshotContest(contest));

        // Chỉ soft delete contest, giữ nguyên tất cả dữ liệu liên quan
        // - contest_submissions: giữ lại để có lịch sử
        // - contest_registrations: giữ lại để có lịch sử
        // - contest_problems: giữ lại để có thể khôi phục contest
        
        contest.setIsDeleted(true);
        contest.setIsPublic(false); // Ẩn khỏi public
        contest.setIsHidden(true);   // Ẩn khỏi danh sách
        contest.setUpdatedAt(Instant.now());
        contestRepository.save(contest);

        recordAudit(
            actorId,
            actorUsername,
            actorRole,
            "CONTEST_DELETE",
            contest.getId(),
            contest.getTitle(),
            beforeState,
            null,
            "Soft delete contest",
            ipAddress,
            userAgent
        );
    }

    @Transactional
        public void addProblemToContest(
            Long contestId,
            ContestProblemRequest request,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        String beforeState = toJson(snapshotContest(contest));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        ProblemEntity problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

        // Validate problem: phải active (status = true)
        if (!Boolean.TRUE.equals(problem.getStatus())) {
            throw new RuntimeException("Problem không active (status = false). Chỉ có thể thêm problem active vào contest");
        }

        // Check if problem already in contest
        Optional<ContestProblemEntity> existing = contestProblemRepository.findByContestIdAndProblemId(contestId, request.getProblemId());
        if (existing.isPresent()) {
            throw new RuntimeException("Problem đã có trong contest");
        }

        ContestProblemKey key = new ContestProblemKey();
        key.setContestId(contestId);
        key.setProblemId(request.getProblemId());

        ContestProblemEntity contestProblem = ContestProblemEntity.builder()
                .id(key)
                .contest(contest)
                .problem(problem)
                .problemOrder(request.getOrder().toUpperCase())
                .points(request.getPoints() != null ? request.getPoints() : 100)
                .build();

        contestProblemRepository.save(contestProblem);
        
        // Tự động set isPublic = false để problem không hiện ở ProblemsPage (contest-only)
        if (Boolean.TRUE.equals(problem.getIsPublic())) {
            problem.setIsPublic(false);
            problemRepository.save(problem);
        }

        recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "CONTEST_ADD_PROBLEM",
                contest.getId(),
                contest.getTitle(),
                beforeState,
                toJson(snapshotContest(contest)),
                "Add problem " + problem.getCode() + " to contest",
                ipAddress,
                userAgent
        );
    }

    @Transactional
        public void removeProblemFromContest(
            Long contestId,
            Long problemId,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
            ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

            String beforeState = toJson(snapshotContest(contest));

        Optional<ContestProblemEntity> contestProblemOpt = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId);
        if (contestProblemOpt.isEmpty()) {
            throw new RuntimeException("Problem không có trong contest");
        }

            String removedProblemCode = contestProblemOpt.get().getProblem() != null
                ? contestProblemOpt.get().getProblem().getCode()
                : String.valueOf(problemId);
        contestProblemRepository.delete(contestProblemOpt.get());

            recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "CONTEST_REMOVE_PROBLEM",
                contest.getId(),
                contest.getTitle(),
                beforeState,
                toJson(snapshotContest(contest)),
                "Remove problem " + removedProblemCode + " from contest",
                ipAddress,
                userAgent
            );
    }

    public List<ContestRegistrationEntity> getContestRegistrations(Long contestId) {
        return contestRegistrationRepository.findByContestId(contestId);
    }

    public Page<ContestResponse> getContests(Pageable pageable, String search, String type) {
        // Admin thấy tất cả contests (cả public và private, kể cả đã xóa và ẩn)
        // Không filter theo isPublic - admin cần thấy tất cả để quản lý
        // Chỉ filter để loại bỏ các contest đã bị xóa (isDeleted = true)
        Specification<ContestEntity> spec = (root, query, cb) -> 
                cb.equal(root.get("isDeleted"), false);
        
        // Filter by search (title)
        if (search != null && !search.trim().isEmpty()) {
            String lowerCaseSearch = search.toLowerCase();
            Specification<ContestEntity> searchSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + lowerCaseSearch + "%");
            spec = spec.and(searchSpec);
        }
        
        // Filter by contest type
        if (type != null && !type.trim().isEmpty()) {
            try {
                ContestType contestType = ContestType.valueOf(type.toUpperCase());
                Specification<ContestEntity> typeSpec = (root, query, cb) ->
                        cb.equal(root.get("contestType"), contestType);
                spec = spec.and(typeSpec);
            } catch (IllegalArgumentException e) {
                // Invalid contest type, ignore filter
            }
        }
        
        return contestRepository.findAll(spec, pageable)
                .map(contest -> mapToContestResponse(contest, null));
    }
    
    @Transactional
        public void toggleContestVisibility(
            Long contestId,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
        ) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

            String beforeState = toJson(snapshotContest(contest));
        
        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }
        
        // Toggle isHidden
        contest.setIsHidden(!Boolean.TRUE.equals(contest.getIsHidden()));
        contest.setUpdatedAt(Instant.now());
        contestRepository.save(contest);

        recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "CONTEST_TOGGLE_VISIBILITY",
                contest.getId(),
                contest.getTitle(),
                beforeState,
                toJson(snapshotContest(contest)),
                Boolean.TRUE.equals(contest.getIsHidden()) ? "Hide contest" : "Show contest",
                ipAddress,
                userAgent
        );
    }

    private java.util.Map<String, Object> snapshotContest(ContestEntity contest) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", contest.getId());
        snapshot.put("title", contest.getTitle());
        snapshot.put("contestType", contest.getContestType() != null ? contest.getContestType().name() : null);
        snapshot.put("durationMinutes", contest.getDurationMinutes());
        snapshot.put("startTime", contest.getStartTime());
        snapshot.put("endTime", contest.getEndTime());
        snapshot.put("isPublic", contest.getIsPublic());
        snapshot.put("isHidden", contest.getIsHidden());
        snapshot.put("isDeleted", contest.getIsDeleted());
        snapshot.put("problemIds", contestProblemRepository.findByContestIdOrderByProblemOrder(contest.getId()).stream()
                .map(cp -> cp.getProblem() != null ? cp.getProblem().getId() : null)
                .collect(Collectors.toList()));
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
                            .objectType("CONTEST")
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
            // Keep contest action successful even if audit log fails
        }
    }

    private ContestResponse mapToContestResponse(ContestEntity contest, Long userId) {
        boolean isRegistered = userId != null && contestRegistrationRepository.existsByContestIdAndUserId(contest.getId(), userId);
        
        int totalProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contest.getId()).size();
        int totalRegistrations = contestRegistrationRepository.findByContestId(contest.getId()).size();

        return ContestResponse.builder()
                .id(contest.getId())
                .title(contest.getTitle())
                .description(contest.getDescription())
                .contestType(contest.getContestType())
                .durationMinutes(contest.getDurationMinutes())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .registrationStartTime(contest.getRegistrationStartTime())
                .registrationEndTime(contest.getRegistrationEndTime())
                .isPublic(contest.getIsPublic())
                .hasAccessCode(contest.getAccessCode() != null && !contest.getAccessCode().trim().isEmpty())
                .isHidden(contest.getIsHidden())
                .status(contest.getStatus())
                .authorId(contest.getAuthor().getId())
                .authorName(contest.getAuthor().getUsername())
                .isRegistered(isRegistered)
                .totalProblems(totalProblems)
                .totalRegistrations(totalRegistrations)
                .build();
    }

    private ContestDetailResponse mapToContestDetailResponse(ContestEntity contest, Long userId) {
        List<ContestProblemEntity> contestProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contest.getId());
        List<ContestProblemResponse> problemResponses = contestProblems.stream()
                .map(cp -> mapToContestProblemResponse(cp, userId))
                .collect(Collectors.toList());

        boolean isRegistered = userId != null && contestRegistrationRepository.existsByContestIdAndUserId(contest.getId(), userId);

        return ContestDetailResponse.builder()
                .id(contest.getId())
                .title(contest.getTitle())
                .description(contest.getDescription())
                .contestType(contest.getContestType())
                .durationMinutes(contest.getDurationMinutes())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .registrationStartTime(contest.getRegistrationStartTime())
                .registrationEndTime(contest.getRegistrationEndTime())
                .isPublic(contest.getIsPublic())
                .hasAccessCode(contest.getAccessCode() != null && !contest.getAccessCode().trim().isEmpty())
                .status(contest.getStatus())
                .authorId(contest.getAuthor().getId())
                .authorName(contest.getAuthor().getUsername())
                .isRegistered(isRegistered)
                .problems(problemResponses)
                .build();
    }

    private ContestProblemResponse mapToContestProblemResponse(ContestProblemEntity cp, Long userId) {
        ProblemEntity problem = cp.getProblem();

        // Map problem to ProblemResponse (simplified)
        ProblemResponse problemResponse = ProblemResponse.builder()
                .id(problem.getId())
                .code(problem.getCode())
                .title(problem.getTitle())
                .level(problem.getLevel())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .authorId(problem.getAuthor() != null ? problem.getAuthor().getId() : null)
                .authorName(problem.getAuthor() != null ? problem.getAuthor().getUsername() : null)
                .build();

        return ContestProblemResponse.builder()
                .problemId(problem.getId())
                .order(cp.getProblemOrder())
                .points(cp.getPoints())
                .problem(problemResponse)
                .isSolved(false)
                .bestScore(0)
                .build();
    }
}

