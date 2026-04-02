package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.request.CreateSubmissionRequest;
import com.hcmute.codesphere_server.model.payload.response.ProblemSolutionResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionResponse;
import com.hcmute.codesphere_server.repository.common.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import com.hcmute.codesphere_server.model.enums.ContestType;
import java.util.Comparator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;
    private final JudgeService judgeService;
    private final EntityManager entityManager;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;
    private final UserProblemBestRepository userProblemBestRepository;

    @Transactional
    public SubmissionDetailResponse createSubmission(CreateSubmissionRequest request, Long userId, Long contestId) {
        // Kiểm tra problem tồn tại và active
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Nếu có contestId, validate contest access
        if (contestId != null) {
            validateContestAccess(contestId, userId);
        }

        // Kiểm tra language tồn tại
        LanguageEntity language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngôn ngữ"));

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Kiểm tra language có được hỗ trợ bởi problem không
        boolean isLanguageSupported = problem.getLanguages().stream()
                .anyMatch(lang -> lang.getId().equals(request.getLanguageId()));
        
        if (!isLanguageSupported) {
            throw new RuntimeException("Ngôn ngữ này không được hỗ trợ cho bài tập này");
        }

        // Tạo submission mới với trạng thái ban đầu
        Instant now = Instant.now();
        SubmissionEntity submission = SubmissionEntity.builder()
                .user(user)
                .problem(problem)
                .language(language)
                .codeContent(request.getCodeContent())
                .isAccepted(false)
                .score(0)
                .statusCode(0) // 0 = PENDING
                .statusRuntime("0 ms")
                .memoryKb(0)
                .displayRuntime("0 ms")
                .totalCorrect(0)
                .totalTestcases(0)
                .statusMemory("0 KB")
                .statusMsg("Đang chờ xử lý...")
                .state("PENDING")
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        submission = submissionRepository.save(submission);
        
        // Flush để đảm bảo submission được lưu vào database trước khi gọi async
        entityManager.flush();
        
        // Lấy ID của submission để truyền vào async method
        Long submissionId = submission.getId();

        // Gọi judge service để chạy code và cập nhật kết quả (async)
        // Đợi transaction commit xong mới gọi judge để tránh lỗi "Submission not found"
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    judgeService.judgeSubmission(submissionId);
                }
            }
        );

        return mapToSubmissionDetailResponse(submission);
    }

    private void validateContestAccess(Long contestId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        Instant now = Instant.now();

        if (contest.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: user phải đã bắt đầu (startedAt != null) và chưa hết thời gian (now < endedAt)
            if (userId == null) {
                throw new RuntimeException("Bạn cần đăng nhập để tham gia contest");
            }
            
            Optional<ContestRegistrationEntity> registrationOpt = 
                    contestRegistrationRepository.findByContestId(contestId)
                    .stream()
                    .filter(reg -> reg.getUser().getId().equals(userId))
                    .findFirst();
            
            if (registrationOpt.isEmpty()) {
                throw new RuntimeException("Bạn chưa bắt đầu contest này. Vui lòng bấm 'Bắt đầu' trước.");
            }
            
            ContestRegistrationEntity registration = registrationOpt.get();
            if (registration.getStartedAt() == null || registration.getEndedAt() == null) {
                throw new RuntimeException("Bạn chưa bắt đầu contest này. Vui lòng bấm 'Bắt đầu' trước.");
            }
            
            if (now.isBefore(registration.getStartedAt())) {
                throw new RuntimeException("Thời gian làm bài của bạn chưa bắt đầu");
            }
            
            if (now.isAfter(registration.getEndedAt())) {
                throw new RuntimeException("Thời gian làm bài của bạn đã hết. Vui lòng bấm 'Làm lại' để bắt đầu session mới.");
            }
        } else if (contest.getContestType() == ContestType.OFFICIAL) {
            // OFFICIAL: user phải đã đăng ký và contest phải đang diễn ra
            if (userId != null && !contestRegistrationRepository.existsByContestIdAndUserId(contestId, userId)) {
                throw new RuntimeException("Bạn chưa đăng ký contest này");
            }

            // Check if contest has started (chỉ cho phép submit khi contest đã bắt đầu)
            if (contest.getStartTime() == null || contest.getEndTime() == null) {
                throw new RuntimeException("Contest không có thời gian hợp lệ");
            }
            
            if (now.isBefore(contest.getStartTime())) {
                throw new RuntimeException("Contest chưa bắt đầu. Bạn chỉ có thể submit khi contest đã bắt đầu");
            }
            
            if (now.isAfter(contest.getEndTime())) {
                throw new RuntimeException("Contest đã kết thúc");
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getSubmissions(
            Long userId,
            Long problemId,
            String search,
            String status,
            Pageable pageable) {
        
        Specification<SubmissionEntity> spec = buildSpecification(userId, problemId, search, status);
        Page<SubmissionEntity> submissions = submissionRepository.findAll(spec, pageable);
        
        return submissions.map(this::mapToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getMySubmissions(Long userId, Pageable pageable) {
        Page<SubmissionEntity> submissions = submissionRepository.findByUserId(userId, pageable);
        return submissions.map(this::mapToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionById(Long id) {
        SubmissionEntity submission = submissionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy submission"));
        
        return mapToSubmissionDetailResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<ProblemSolutionResponse> getProblemSolutions(Long problemId, Long currentUserId, String search, String languageCode, int offset, int limit) {
        problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        UserProblemBestEntity myBest = userProblemBestRepository
                .findByUserIdAndProblemId(currentUserId, problemId)
                .orElseThrow(() -> new RuntimeException("Bạn cần giải đúng bài này trước khi xem Solutions"));

        if (myBest.getBestSubmission() == null || !Boolean.TRUE.equals(myBest.getBestSubmission().getIsAccepted())) {
            throw new RuntimeException("Bạn cần giải đúng bài này trước khi xem Solutions");
        }

        String keyword = search == null ? "" : search.trim().toLowerCase();
        String langFilter = languageCode == null ? "" : languageCode.trim().toLowerCase();

        List<UserProblemBestEntity> acceptedBest = userProblemBestRepository
                .findAllByProblemIdOrderByBestScoreDesc(problemId)
                .stream()
                .filter(upb -> upb.getBestSubmission() != null)
                .filter(upb -> Boolean.TRUE.equals(upb.getBestSubmission().getIsAccepted()))
                .filter(upb -> !upb.getUser().getId().equals(currentUserId))
                .filter(upb -> keyword.isEmpty() || (upb.getUser() != null
                        && upb.getUser().getUsername() != null
                        && upb.getUser().getUsername().toLowerCase().contains(keyword)))
                .filter(upb -> langFilter.isEmpty() || (upb.getBestSubmission().getLanguage() != null
                        && upb.getBestSubmission().getLanguage().getCode() != null
                        && upb.getBestSubmission().getLanguage().getCode().toLowerCase().contains(langFilter)))
                .sorted(Comparator
                        .comparing(UserProblemBestEntity::getBestScore, Comparator.reverseOrder())
                        .thenComparing(upb -> upb.getBestSubmission().getCreatedAt()))
                .skip(offset)
                .limit(Math.max(1, limit))
                .toList();

        List<ProblemSolutionResponse> result = new ArrayList<>();
        for (int i = 0; i < acceptedBest.size(); i++) {
            UserProblemBestEntity entry = acceptedBest.get(i);
            SubmissionEntity bestSubmission = entry.getBestSubmission();

            result.add(ProblemSolutionResponse.builder()
                    .submissionId(bestSubmission.getId())
                    .userId(entry.getUser().getId())
                    .username(entry.getUser().getUsername())
                    .rank(i + offset + 1)
                    .bestScore(entry.getBestScore())
                    .languageName(bestSubmission.getLanguage() != null ? bestSubmission.getLanguage().getName() : null)
                    .languageCode(bestSubmission.getLanguage() != null ? bestSubmission.getLanguage().getCode() : null)
                    .statusRuntime(bestSubmission.getStatusRuntime())
                    .statusMemory(bestSubmission.getStatusMemory())
                    .submittedAt(bestSubmission.getCreatedAt())
                    .codeContent(bestSubmission.getCodeContent())
                    .build());
        }

        return result;
    }

    private Specification<SubmissionEntity> buildSpecification(
            Long userId,
            Long problemId,
            String search,
            String status) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Chỉ lấy submission chưa bị xóa
            predicates.add(cb.equal(root.get("isDeleted"), false));
            
            // Filter theo userId
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            
            // Filter theo problemId
            if (problemId != null) {
                predicates.add(cb.equal(root.get("problem").get("id"), problemId));
            }

            // Filter theo problem title/code
            if (search != null && !search.trim().isEmpty()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("problem").get("title")), keyword),
                        cb.like(cb.lower(root.get("problem").get("code")), keyword)
                ));
            }
            
            // Filter chính xác theo state
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("state"), status.toUpperCase()));
            }
            
            // Loại bỏ submission đã được dùng trong contest (chỉ hiện submission non-contest)
            // LEFT JOIN với ContestSubmissionEntity và filter WHERE contest_submission.id IS NULL
            Subquery<Long> contestSubmissionSubquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<ContestSubmissionEntity> contestSubmissionRoot = contestSubmissionSubquery.from(ContestSubmissionEntity.class);
            contestSubmissionSubquery.select(contestSubmissionRoot.get("submission").get("id"));
            contestSubmissionSubquery.where(cb.equal(contestSubmissionRoot.get("submission").get("id"), root.get("id")));
            
            predicates.add(cb.not(cb.exists(contestSubmissionSubquery)));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SubmissionResponse mapToSubmissionResponse(SubmissionEntity entity) {
        return SubmissionResponse.builder()
                .id(entity.getId())
                .problemId(entity.getProblem() != null ? entity.getProblem().getId() : null)
                .problemTitle(entity.getProblem() != null ? entity.getProblem().getTitle() : null)
                .problemCode(entity.getProblem() != null ? entity.getProblem().getCode() : null)
                .languageId(entity.getLanguage() != null ? entity.getLanguage().getId() : null)
                .languageName(entity.getLanguage() != null ? entity.getLanguage().getName() : null)
                .languageCode(entity.getLanguage() != null ? entity.getLanguage().getCode() : null)
                .isAccepted(entity.getIsAccepted())
                .score(entity.getScore())
                .statusMsg(entity.getStatusMsg())
                .statusRuntime(entity.getStatusRuntime())
                .statusMemory(entity.getStatusMemory())
                .totalCorrect(entity.getTotalCorrect())
                .totalTestcases(entity.getTotalTestcases())
                .state(entity.getState())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private SubmissionDetailResponse mapToSubmissionDetailResponse(SubmissionEntity entity) {
        return SubmissionDetailResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .problemId(entity.getProblem() != null ? entity.getProblem().getId() : null)
                .problemTitle(entity.getProblem() != null ? entity.getProblem().getTitle() : null)
                .problemCode(entity.getProblem() != null ? entity.getProblem().getCode() : null)
                .languageId(entity.getLanguage() != null ? entity.getLanguage().getId() : null)
                .languageName(entity.getLanguage() != null ? entity.getLanguage().getName() : null)
                .languageCode(entity.getLanguage() != null ? entity.getLanguage().getCode() : null)
                .codeContent(entity.getCodeContent())
                .isAccepted(entity.getIsAccepted())
                .score(entity.getScore())
                .statusCode(entity.getStatusCode())
                .statusRuntime(entity.getStatusRuntime())
                .displayRuntime(entity.getDisplayRuntime())
                .memoryKb(entity.getMemoryKb())
                .statusMemory(entity.getStatusMemory())
                .statusMsg(entity.getStatusMsg())
                .state(entity.getState())
                .totalCorrect(entity.getTotalCorrect())
                .totalTestcases(entity.getTotalTestcases())
                .compileError(entity.getCompileError())
                .fullCompileError(entity.getFullCompileError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

