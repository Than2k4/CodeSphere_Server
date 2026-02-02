package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.entity.embedded.ContestRegistrationKey;
import com.hcmute.codesphere_server.model.enums.ContestType;
import com.hcmute.codesphere_server.model.payload.request.RegisterContestRequest;
import com.hcmute.codesphere_server.model.payload.response.*;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final NotificationService notificationService;

    public Page<ContestResponse> getContests(Pageable pageable, Boolean isPublic, String status, String contestType, Long userId) {
        Specification<ContestEntity> spec = Specification.where(null);

        // Loại bỏ contests đã xóa
        spec = spec.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));
        
        // Loại bỏ contests bị ẩn (chỉ admin mới thấy contests ẩn)
        spec = spec.and((root, query, cb) -> cb.equal(root.get("isHidden"), false));

        // Filter by public/private
        if (isPublic != null) {
            if (isPublic) {
                // Chỉ lấy public contests
                Specification<ContestEntity> publicSpec = (root, query, cb) ->
                        cb.equal(root.get("isPublic"), true);
                spec = spec.and(publicSpec);
            } else {
                // isPublic = false: chỉ lấy private contests mà user đã register
                if (userId != null) {
                    List<Long> registeredContestIds = contestRegistrationRepository.findByUserId(userId)
                            .stream()
                            .map(cr -> cr.getContest().getId())
                            .collect(Collectors.toList());
                    
                    if (registeredContestIds.isEmpty()) {
                        // Nếu user chưa register private contest nào, trả về empty
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), -1L)); // Always false condition
                    } else {
                        // Chỉ trả về private contests mà user đã register
                        Specification<ContestEntity> privateSpec = (root, query, cb) ->
                                cb.and(
                                        cb.equal(root.get("isPublic"), false),
                                        root.get("id").in(registeredContestIds)
                                );
                        spec = spec.and(privateSpec);
                    }
                } else {
                    // User chưa đăng nhập, không trả về private contests
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), -1L)); // Always false condition
                }
            }
        } else {
            // Nếu không filter isPublic (lấy cả public và private)
            // Chỉ trả về private contests nếu user đã register
            if (userId != null) {
                // Lấy danh sách contest IDs mà user đã register
                List<Long> registeredContestIds = contestRegistrationRepository.findByUserId(userId)
                        .stream()
                        .map(cr -> cr.getContest().getId())
                        .collect(Collectors.toList());
                
                // Filter: public contests HOẶC private contests mà user đã register
                Specification<ContestEntity> visibilitySpec = (root, query, cb) -> {
                    if (registeredContestIds.isEmpty()) {
                        // Nếu user chưa register contest nào, chỉ trả về public
                        return cb.equal(root.get("isPublic"), true);
                    } else {
                        // Trả về public HOẶC private contests mà user đã register
                        return cb.or(
                                cb.equal(root.get("isPublic"), true),
                                cb.and(
                                        cb.equal(root.get("isPublic"), false),
                                        root.get("id").in(registeredContestIds)
                                )
                        );
                    }
                };
                spec = spec.and(visibilitySpec);
            } else {
                // Nếu user chưa đăng nhập, chỉ trả về public contests
                spec = spec.and((root, query, cb) -> cb.equal(root.get("isPublic"), true));
            }
        }

        // Filter by contestType
        if (contestType != null && !contestType.trim().isEmpty()) {
            Specification<ContestEntity> typeSpec = (root, query, cb) ->
                    cb.equal(root.get("contestType"), ContestType.valueOf(contestType.toUpperCase()));
            spec = spec.and(typeSpec);
        }

        // Filter by status
        Instant now = Instant.now();
        if (status != null && !status.trim().isEmpty()) {
            if ("UPCOMING".equalsIgnoreCase(status)) {
                Specification<ContestEntity> statusSpec = (root, query, cb) ->
                        cb.and(
                                cb.isNotNull(root.get("startTime")),
                                cb.greaterThan(root.get("startTime"), now)
                        );
                spec = spec.and(statusSpec);
            } else if ("ONGOING".equalsIgnoreCase(status)) {
                Specification<ContestEntity> statusSpec = (root, query, cb) ->
                        cb.and(
                                cb.isNotNull(root.get("startTime")),
                                cb.isNotNull(root.get("endTime")),
                                cb.lessThanOrEqualTo(root.get("startTime"), now),
                                cb.greaterThanOrEqualTo(root.get("endTime"), now)
                        );
                spec = spec.and(statusSpec);
            } else if ("ENDED".equalsIgnoreCase(status)) {
                Specification<ContestEntity> statusSpec = (root, query, cb) ->
                        cb.and(
                                cb.isNotNull(root.get("endTime")),
                                cb.lessThan(root.get("endTime"), now)
                        );
                spec = spec.and(statusSpec);
            } else if ("AVAILABLE".equalsIgnoreCase(status)) {
                // AVAILABLE chỉ cho PRACTICE contests
                Specification<ContestEntity> statusSpec = (root, query, cb) ->
                        cb.equal(root.get("contestType"), ContestType.PRACTICE);
                spec = spec.and(statusSpec);
            }
        }

        Page<ContestEntity> contests = contestRepository.findAll(spec, pageable);

        return contests.map(contest -> mapToContestResponse(contest, userId));
    }

    public ContestDetailResponse getContestById(Long contestId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        List<ContestProblemEntity> contestProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contestId);
        List<ContestProblemResponse> problemResponses = contestProblems.stream()
                .map(cp -> mapToContestProblemResponse(cp, contestId, userId))
                .collect(Collectors.toList());

        boolean isRegistered = userId != null && contestRegistrationRepository.existsByContestIdAndUserId(contestId, userId);
        int totalRegistrations = contestRegistrationRepository.findByContestId(contestId).size();

        // Lấy registration info để trả về startedAt và endedAt cho PRACTICE contest
        Instant startedAt = null;
        Instant endedAt = null;
        if (userId != null && contest.getContestType() == ContestType.PRACTICE) {
            Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                    .findByContestId(contestId)
                    .stream()
                    .filter(reg -> reg.getUser().getId().equals(userId))
                    .findFirst();
            if (registrationOpt.isPresent()) {
                ContestRegistrationEntity reg = registrationOpt.get();
                startedAt = reg.getStartedAt();
                endedAt = reg.getEndedAt();
            }
        }

        // Ẩn problems dựa trên loại contest
        Instant now = Instant.now();
        boolean canViewProblems = false;
        
        if (contest.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: user phải đã bắt đầu (startedAt != null) và (đang làm hoặc đã hết thời gian để hiển thị điểm)
            canViewProblems = startedAt != null && endedAt != null && 
                             (now.isAfter(startedAt) && now.isBefore(endedAt) || now.isAfter(endedAt));
        } else if (contest.getContestType() == ContestType.OFFICIAL) {
            // OFFICIAL: contest phải đã bắt đầu (bao gồm cả khi đã kết thúc để hiển thị điểm)
            canViewProblems = contest.getStartTime() != null && 
                             contest.getEndTime() != null &&
                             now.isAfter(contest.getStartTime());
        }
        
        List<ContestProblemResponse> visibleProblems = canViewProblems ? problemResponses : new ArrayList<>();

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
                .totalRegistrations(totalRegistrations)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .problems(visibleProblems)
                .build();
    }

    @Transactional
    public ContestResponse verifyAccessCodeAndGetContest(String accessCode, Long userId) {
        // Tìm contest có access code khớp
        List<ContestEntity> contests = contestRepository.findAll()
                .stream()
                .filter(c -> !c.getIsDeleted() && 
                             !c.getIsHidden() &&
                             !c.getIsPublic() &&
                             c.getAccessCode() != null &&
                             c.getAccessCode().equals(accessCode))
                .collect(Collectors.toList());
        
        if (contests.isEmpty()) {
            throw new RuntimeException("Invalid access code or contest not found");
        }
        
        ContestEntity contest = contests.get(0);
        
        // Kiểm tra user đã register chưa, nếu chưa thì tự động register
        if (userId != null && !contestRegistrationRepository.existsByContestIdAndUserId(contest.getId(), userId)) {
            // Tự động register user vào contest
            RegisterContestRequest registerRequest = new RegisterContestRequest();
            registerRequest.setAccessCode(accessCode);
            try {
                registerContest(contest.getId(), userId, registerRequest);
            } catch (Exception e) {
                // Nếu register fail, vẫn trả về contest để user có thể thử lại
            }
        }
        
        return mapToContestResponse(contest, userId);
    }

    @Transactional
    public void registerContest(Long contestId, Long userId, RegisterContestRequest request) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // PRACTICE contest không cần đăng ký
        if (contest.getContestType() == ContestType.PRACTICE) {
            throw new RuntimeException("PRACTICE contest không cần đăng ký. Vui lòng bấm 'Bắt đầu' để bắt đầu làm bài.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Check if already registered
        if (contestRegistrationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new RuntimeException("Bạn đã đăng ký contest này rồi");
        }

        // OFFICIAL contest: phải đăng ký trước khi contest bắt đầu
        Instant now = Instant.now();
        if (contest.getStartTime() != null && now.isAfter(contest.getStartTime())) {
            throw new RuntimeException("Đã hết thời gian đăng ký. Contest đã bắt đầu.");
        }

        // Check access code for private contests
        if (!contest.getIsPublic()) {
            if (request.getAccessCode() == null || request.getAccessCode().trim().isEmpty()) {
                throw new RuntimeException("Contest này yêu cầu mã truy cập");
            }
            if (!contest.getAccessCode().equals(request.getAccessCode())) {
                throw new RuntimeException("Mã truy cập không đúng");
            }
        }

        // Create registration
        ContestRegistrationKey key = new ContestRegistrationKey();
        key.setContestId(contestId);
        key.setUserId(userId);

        ContestRegistrationEntity registration = ContestRegistrationEntity.builder()
                .id(key)
                .contest(contest)
                .user(user)
                .registeredAt(Instant.now())
                .build();

        contestRegistrationRepository.save(registration);
        
        // Gửi notification ngay nếu contest sắp bắt đầu (trong vòng 10 phút)
        if (contest.getStartTime() != null) {
            long minutesUntilStart = ChronoUnit.MINUTES.between(now, contest.getStartTime());
            
            // Nếu contest còn <= 10 phút nữa bắt đầu, gửi notification ngay
            if (minutesUntilStart > 0 && minutesUntilStart <= 10) {
                try {
                    // Kiểm tra xem đã gửi notification chưa (tránh gửi trùng)
                    Instant veryLongAgo = now.minus(365, ChronoUnit.DAYS);
                    boolean alreadyNotified = notificationService.hasContestNotification(
                            userId,
                            contestId,
                            veryLongAgo
                    );
                    
                    if (!alreadyNotified) {
                        String title = "Contest Starting Soon";
                        String content = String.format("Contest '%s' will start in %d minute(s). Get ready!",
                                contest.getTitle(),
                                minutesUntilStart);
                        
                        notificationService.createContestNotification(
                                userId,
                                title,
                                content,
                                contestId
                        );
                        System.out.println("Sent immediate contest reminder to user " + userId + 
                                " for contest " + contestId + " (" + minutesUntilStart + " minutes before start)");
                    }
                } catch (Exception e) {
                    // Log error nhưng không throw (đăng ký vẫn thành công)
                    System.err.println("Error sending immediate contest notification to user " + userId + 
                            " for contest " + contestId + ": " + e.getMessage());
                }
            }
        }
        
        // Gửi notification ngay nếu contest sắp bắt đầu (trong vòng 10 phút)
        if (contest.getStartTime() != null) {
            long minutesUntilStart = ChronoUnit.MINUTES.between(now, contest.getStartTime());
            
            // Nếu contest còn <= 10 phút nữa bắt đầu, gửi notification ngay
            if (minutesUntilStart > 0 && minutesUntilStart <= 10) {
                try {
                    // Kiểm tra xem đã gửi notification chưa (tránh gửi trùng)
                    Instant veryLongAgo = now.minus(365, ChronoUnit.DAYS);
                    boolean alreadyNotified = notificationService.hasContestNotification(
                            userId,
                            contestId,
                            veryLongAgo
                    );
                    
                    if (!alreadyNotified) {
                        String title = "Contest Starting Soon";
                        String content = String.format("Contest '%s' will start in %d minute(s). Get ready!",
                                contest.getTitle(),
                                minutesUntilStart);
                        
                        notificationService.createContestNotification(
                                userId,
                                title,
                                content,
                                contestId
                        );
                        System.out.println("Sent immediate contest reminder to user " + userId + 
                                " for contest " + contestId + " (" + minutesUntilStart + " minutes before start)");
                    }
                } catch (Exception e) {
                    // Log error nhưng không throw (đăng ký vẫn thành công)
                    System.err.println("Error sending immediate contest notification to user " + userId + 
                            " for contest " + contestId + ": " + e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void startContest(Long contestId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // Chỉ PRACTICE contest mới có thể start
        if (contest.getContestType() != ContestType.PRACTICE) {
            throw new RuntimeException("Chỉ PRACTICE contest mới có thể bắt đầu bằng cách này");
        }

        if (contest.getDurationMinutes() == null || contest.getDurationMinutes() <= 0) {
            throw new RuntimeException("Contest không có thời gian làm bài hợp lệ");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tìm hoặc tạo registration
        Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                .findByContestId(contestId)
                .stream()
                .filter(reg -> reg.getUser().getId().equals(userId))
                .findFirst();

        ContestRegistrationEntity registration;
        Instant now = Instant.now();
        
        if (registrationOpt.isPresent()) {
            registration = registrationOpt.get();
            // Nếu đã hết thời gian hoặc chưa bắt đầu, tạo session mới (làm lại)
            if (registration.getEndedAt() == null || now.isAfter(registration.getEndedAt())) {
                // KHÔNG xóa submissions cũ - giữ lại để có thể so sánh best attempt
                // Chỉ reset session thời gian
                registration.setStartedAt(now);
                registration.setEndedAt(now.plus(contest.getDurationMinutes(), ChronoUnit.MINUTES));
                registration.setAttemptCount(registration.getAttemptCount() != null ? registration.getAttemptCount() + 1 : 1);
            } else {
                // Đang trong thời gian làm, không cần làm gì
                return;
            }
        } else {
            // Tạo registration mới
            ContestRegistrationKey key = new ContestRegistrationKey();
            key.setContestId(contestId);
            key.setUserId(userId);
            
            registration = ContestRegistrationEntity.builder()
                    .id(key)
                    .contest(contest)
                    .user(user)
                    .registeredAt(now)
                    .startedAt(now)
                    .endedAt(now.plus(contest.getDurationMinutes(), ChronoUnit.MINUTES))
                    .attemptCount(1)
                    .build();
        }

        contestRegistrationRepository.save(registration);
    }

    @Transactional
    public void finishContest(Long contestId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // Tìm registration
        Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                .findByContestId(contestId)
                .stream()
                .filter(reg -> reg.getUser().getId().equals(userId))
                .findFirst();

        if (registrationOpt.isEmpty()) {
            throw new RuntimeException("Bạn chưa đăng ký contest này");
        }

        ContestRegistrationEntity registration = registrationOpt.get();
        
        Instant now = Instant.now();
        
        // Xử lý khác nhau cho PRACTICE và OFFICIAL
        if (contest.getContestType() == ContestType.PRACTICE) {
            // PRACTICE: time tính chạy từ khi user bắt đầu
            // startedAt = khi user bắt đầu (đã set trong startContest)
            // endedAt = startedAt + duration (tính chạy) hoặc now nếu hoàn thành sớm
            if (registration.getStartedAt() == null) {
                throw new RuntimeException("Bạn chưa bắt đầu contest này");
            }
            
            // Set endedAt = now để lưu thời gian hoàn thành sớm
            // Nếu đã hết thời gian tự động (endedAt đã được set = startedAt + duration), không cần set lại
            if (registration.getEndedAt() != null && now.isAfter(registration.getEndedAt())) {
                // Đã hết thời gian (tính chạy), nhưng vẫn cần tính và cập nhật best attempt
                // (không set lại endedAt, tiếp tục tính toán với endedAt = startedAt + duration)
            } else {
                // Hoàn thành sớm: Set endedAt = now (thời gian thực khi hoàn thành)
                registration.setEndedAt(now);
            }
        } else if (contest.getContestType() == ContestType.OFFICIAL) {
            // OFFICIAL: time = thời gian thực (tất cả user cùng lúc)
            // startedAt = contest.startTime (thời gian thực)
            // endedAt = now (thời gian thực khi hoàn thành) hoặc contest.endTime (khi hết giờ)
            if (contest.getStartTime() == null || contest.getEndTime() == null) {
                throw new RuntimeException("Contest không có thời gian bắt đầu/kết thúc");
            }
            
            Instant contestStart = contest.getStartTime();
            Instant contestEnd = contest.getEndTime();
            
            if (now.isBefore(contestStart)) {
                throw new RuntimeException("Contest chưa bắt đầu");
            }
            
            if (now.isAfter(contestEnd)) {
                throw new RuntimeException("Contest đã kết thúc");
            }
            
            // OFFICIAL: startedAt = contest.startTime (thời gian thực, tất cả user cùng lúc)
            if (registration.getStartedAt() == null) {
                registration.setStartedAt(contestStart);
            }
            
            // OFFICIAL: endedAt = now (thời gian thực khi hoàn thành sớm)
            registration.setEndedAt(now);
        }

        // Tính toán điểm và thời gian của attempt hiện tại
        if (registration.getStartedAt() != null && registration.getEndedAt() != null) {
            // Tính totalScore từ submissions trong attempt hiện tại
            List<ContestSubmissionEntity> currentAttemptSubmissions = contestSubmissionRepository
                    .findByContestIdAndUserId(contestId, userId)
                    .stream()
                    .filter(cs -> {
                        // Chỉ lấy submissions trong khoảng thời gian của attempt hiện tại
                        Instant subTime = cs.getSubmittedAt();
                        return (subTime.isAfter(registration.getStartedAt()) || subTime.equals(registration.getStartedAt()))
                                && (subTime.isBefore(registration.getEndedAt()) || subTime.equals(registration.getEndedAt()));
                    })
                    .collect(Collectors.toList());

            // Tính best score cho mỗi problem
            List<ContestProblemEntity> contestProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contestId);
            Map<Long, String> problemOrderMap = contestProblems.stream()
                    .collect(Collectors.toMap(cp -> cp.getProblem().getId(), ContestProblemEntity::getProblemOrder));
            
            Map<String, Integer> problemScores = new HashMap<>();
            int currentTotalScore = 0;
            
            for (ContestSubmissionEntity cs : currentAttemptSubmissions) {
                Long problemId = cs.getSubmission().getProblem().getId();
                String order = problemOrderMap.get(problemId);
                
                if (order != null) {
                    Integer currentScore = problemScores.get(order);
                    Integer submissionScore = cs.getScore() != null ? cs.getScore() : (cs.getSubmission().getIsAccepted() ? 100 : 0);
                    
                    if (currentScore == null || submissionScore > currentScore) {
                        currentTotalScore = currentTotalScore - (currentScore != null ? currentScore : 0) + submissionScore;
                        problemScores.put(order, submissionScore);
                    }
                }
            }

            // Tính completionTimeSeconds
            long currentCompletionTimeSeconds = java.time.Duration.between(registration.getStartedAt(), registration.getEndedAt()).getSeconds();
            
            // So sánh với best attempt và cập nhật nếu tốt hơn
            boolean isBetter = false;
            if (registration.getBestTotalScore() == null) {
                // Chưa có best attempt
                isBetter = true;
            } else if (currentTotalScore > registration.getBestTotalScore()) {
                // Điểm cao hơn
                isBetter = true;
            } else if (currentTotalScore == registration.getBestTotalScore()) {
                // Cùng điểm, so sánh thời gian (nhanh hơn = tốt hơn)
                if (registration.getBestCompletionTimeSeconds() == null 
                        || currentCompletionTimeSeconds < registration.getBestCompletionTimeSeconds()) {
                    isBetter = true;
                }
            }
            
            if (isBetter) {
                registration.setBestTotalScore(currentTotalScore);
                registration.setBestCompletionTimeSeconds(currentCompletionTimeSeconds);
                registration.setBestStartedAt(registration.getStartedAt());
                registration.setBestEndedAt(registration.getEndedAt());
            }
        }

        contestRegistrationRepository.save(registration);
    }

    public List<ContestProblemResponse> getContestProblems(Long contestId, Long userId) {
        List<ContestProblemEntity> contestProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contestId);
        return contestProblems.stream()
                .map(cp -> mapToContestProblemResponse(cp, contestId, userId))
                .collect(Collectors.toList());
    }

    public List<ContestLeaderboardResponse> getContestLeaderboard(Long contestId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));
        
        Instant now = Instant.now();
        
        // Get all registrations for this contest
        List<ContestRegistrationEntity> registrations = contestRegistrationRepository.findByContestId(contestId);
        
        // Get all problems in contest
        List<ContestProblemEntity> contestProblems = contestProblemRepository.findByContestIdOrderByProblemOrder(contestId);
        Map<Long, String> problemOrderMap = contestProblems.stream()
                .collect(Collectors.toMap(cp -> cp.getProblem().getId(), ContestProblemEntity::getProblemOrder));

        // Calculate scores for each user
        Map<Long, ContestLeaderboardResponse> leaderboardMap = new HashMap<>();

        for (ContestRegistrationEntity registration : registrations) {
            Long userId = registration.getUser().getId();
            String username = registration.getUser().getUsername();

            Map<String, Integer> problemScores = new HashMap<>();
            int totalScore = 0;
            Instant lastSubmissionTime = null;
            int totalSubmissions = 0;
            Instant completedAt = null;
            Long completionTimeSeconds = null;

            // Get all submissions for this user in this contest
            List<ContestSubmissionEntity> allContestSubmissions = contestSubmissionRepository.findByContestIdAndUserId(contestId, userId);
            
            // Kiểm tra xem user có đang làm bài không (PRACTICE contest)
            boolean isCurrentlyActive = contest.getContestType() == ContestType.PRACTICE
                    && registration.getStartedAt() != null
                    && registration.getEndedAt() != null
                    && now.isAfter(registration.getStartedAt())
                    && now.isBefore(registration.getEndedAt());
            
            if (contest.getContestType() == ContestType.PRACTICE && registration.getBestStartedAt() != null && registration.getBestEndedAt() != null && !isCurrentlyActive) {
                // PRACTICE: Đã finish - Sử dụng best attempt - chỉ tính submissions trong khoảng thời gian của best attempt
                Instant bestStart = registration.getBestStartedAt();
                Instant bestEnd = registration.getBestEndedAt();
                
                List<ContestSubmissionEntity> bestAttemptSubmissions = allContestSubmissions.stream()
                        .filter(cs -> {
                            Instant subTime = cs.getSubmittedAt();
                            return (subTime.isAfter(bestStart) || subTime.equals(bestStart))
                                    && (subTime.isBefore(bestEnd) || subTime.equals(bestEnd));
                        })
                        .collect(Collectors.toList());
                
                for (ContestSubmissionEntity cs : bestAttemptSubmissions) {
                    SubmissionEntity submission = cs.getSubmission();
                    Long problemId = submission.getProblem().getId();
                    String order = problemOrderMap.get(problemId);
                    
                    if (order != null) {
                        Integer currentScore = problemScores.get(order);
                        Integer submissionScore = cs.getScore() != null ? cs.getScore() : (submission.getIsAccepted() ? 100 : 0);
                        
                        if (currentScore == null || submissionScore > currentScore) {
                            problemScores.put(order, submissionScore);
                            totalScore = totalScore - (currentScore != null ? currentScore : 0) + submissionScore;
                        }

                        if (lastSubmissionTime == null || cs.getSubmittedAt().isAfter(lastSubmissionTime)) {
                            lastSubmissionTime = cs.getSubmittedAt();
                        }
                        totalSubmissions++;
                    }
                }
                
                // Sử dụng best attempt info
                totalScore = registration.getBestTotalScore() != null ? registration.getBestTotalScore() : totalScore;
                completionTimeSeconds = registration.getBestCompletionTimeSeconds();
                completedAt = registration.getBestEndedAt();
            } else if (isCurrentlyActive) {
                // PRACTICE: Đang làm bài - tính real-time từ submissions trong attempt hiện tại
                Instant currentStart = registration.getStartedAt();
                Instant currentEnd = registration.getEndedAt();
                
                List<ContestSubmissionEntity> currentAttemptSubmissions = allContestSubmissions.stream()
                        .filter(cs -> {
                            Instant subTime = cs.getSubmittedAt();
                            return (subTime.isAfter(currentStart) || subTime.equals(currentStart))
                                    && (subTime.isBefore(currentEnd) || subTime.equals(currentEnd));
                        })
                        .collect(Collectors.toList());
                
                for (ContestSubmissionEntity cs : currentAttemptSubmissions) {
                    SubmissionEntity submission = cs.getSubmission();
                    Long problemId = submission.getProblem().getId();
                    String order = problemOrderMap.get(problemId);
                    
                    if (order != null) {
                        Integer currentScore = problemScores.get(order);
                        Integer submissionScore = cs.getScore() != null ? cs.getScore() : (submission.getIsAccepted() ? 100 : 0);
                        
                        if (currentScore == null || submissionScore > currentScore) {
                            problemScores.put(order, submissionScore);
                            totalScore = totalScore - (currentScore != null ? currentScore : 0) + submissionScore;
                        }

                        if (lastSubmissionTime == null || cs.getSubmittedAt().isAfter(lastSubmissionTime)) {
                            lastSubmissionTime = cs.getSubmittedAt();
                        }
                        totalSubmissions++;
                    }
                }
                
                // Tính completedAt và completionTimeSeconds cho attempt hiện tại
                if (registration.getEndedAt() != null && registration.getStartedAt() != null) {
                    Instant expectedEndTime = registration.getStartedAt().plus(contest.getDurationMinutes(), java.time.temporal.ChronoUnit.MINUTES);
                    
                    if (registration.getEndedAt().isBefore(expectedEndTime)) {
                        completedAt = registration.getEndedAt();
                        long seconds = java.time.Duration.between(registration.getStartedAt(), registration.getEndedAt()).getSeconds();
                        completionTimeSeconds = seconds;
                    } else {
                        completedAt = registration.getEndedAt();
                        completionTimeSeconds = null;
                    }
                } else if (lastSubmissionTime != null) {
                    completedAt = lastSubmissionTime;
                }
            } else {
                // OFFICIAL hoặc PRACTICE chưa có best attempt: tính từ tất cả submissions
                for (ContestSubmissionEntity cs : allContestSubmissions) {
                    SubmissionEntity submission = cs.getSubmission();
                    Long problemId = submission.getProblem().getId();
                    String order = problemOrderMap.get(problemId);
                    
                    if (order != null) {
                        Integer currentScore = problemScores.get(order);
                        Integer submissionScore = cs.getScore() != null ? cs.getScore() : (submission.getIsAccepted() ? 100 : 0);
                        
                        if (currentScore == null || submissionScore > currentScore) {
                            problemScores.put(order, submissionScore);
                            totalScore = totalScore - (currentScore != null ? currentScore : 0) + submissionScore;
                        }

                        if (lastSubmissionTime == null || cs.getSubmittedAt().isAfter(lastSubmissionTime)) {
                            lastSubmissionTime = cs.getSubmittedAt();
                        }
                        totalSubmissions++;
                    }
                }

                // Tính completedAt và completionTimeSeconds
                if (contest.getContestType() == ContestType.PRACTICE) {
                    // PRACTICE: chỉ tính thời gian hoàn thành nếu user đã nhấn "Hoàn thành"
                    if (registration.getEndedAt() != null && registration.getStartedAt() != null) {
                        Instant expectedEndTime = registration.getStartedAt().plus(contest.getDurationMinutes(), java.time.temporal.ChronoUnit.MINUTES);
                        
                        if (registration.getEndedAt().isBefore(expectedEndTime)) {
                            completedAt = registration.getEndedAt();
                            long seconds = java.time.Duration.between(registration.getStartedAt(), registration.getEndedAt()).getSeconds();
                            completionTimeSeconds = seconds;
                        } else {
                            completedAt = registration.getEndedAt();
                            completionTimeSeconds = null;
                        }
                    } else if (lastSubmissionTime != null) {
                        completedAt = lastSubmissionTime;
                    }
                } else {
                    // OFFICIAL: completedAt = thời gian nộp (lastSubmissionTime hoặc endTime)
                    // Không tính completionTimeSeconds (vì không cần thời gian đã dùng, chỉ cần thời gian nộp)
                    if (lastSubmissionTime != null) {
                        completedAt = lastSubmissionTime;
                    } else if (contest.getEndTime() != null) {
                        completedAt = contest.getEndTime();
                    }
                    // OFFICIAL: không set completionTimeSeconds (null)
                    completionTimeSeconds = null;
                }
                
                // Nếu có submission, ưu tiên lastSubmissionTime
                if (lastSubmissionTime != null && completedAt != null && lastSubmissionTime.isAfter(completedAt)) {
                    completedAt = lastSubmissionTime;
                }
            }

            ContestLeaderboardResponse entry = ContestLeaderboardResponse.builder()
                    .userId(userId)
                    .username(username)
                    .totalScore(totalScore)
                    .problemScores(problemScores)
                    .completedAt(completedAt)
                    .completionTimeSeconds(completionTimeSeconds)
                    .lastSubmissionTime(lastSubmissionTime)
                    .totalSubmissions(totalSubmissions)
                    .build();

            leaderboardMap.put(userId, entry);
        }

        // Sort by total score descending, then by completionTimeSeconds ascending (làm nhanh hơn xếp trên)
        // Nếu không có completionTimeSeconds thì sort theo completedAt
        List<ContestLeaderboardResponse> leaderboard = new ArrayList<>(leaderboardMap.values());
        leaderboard.sort((a, b) -> {
            // Sort theo điểm: điểm cao hơn xếp trên
            int scoreCompare = Integer.compare(b.getTotalScore(), a.getTotalScore());
            if (scoreCompare != 0) return scoreCompare;
            
            // Nếu cùng điểm, sort theo thời gian tùy loại contest
            if (contest.getContestType() == ContestType.PRACTICE) {
                // PRACTICE: sort theo completionTimeSeconds (nhanh hơn = tốt hơn)
                if (a.getCompletionTimeSeconds() != null && b.getCompletionTimeSeconds() != null) {
                    return Long.compare(a.getCompletionTimeSeconds(), b.getCompletionTimeSeconds());
                }
                // Nếu một trong hai không có completionTimeSeconds, sort theo completedAt
                if (a.getCompletionTimeSeconds() == null && b.getCompletionTimeSeconds() == null) {
                    // Cả hai đều không có → sort theo completedAt
                    if (a.getCompletedAt() == null) return 1;
                    if (b.getCompletedAt() == null) return -1;
                    return a.getCompletedAt().compareTo(b.getCompletedAt());
                }
                // Có completionTimeSeconds xếp trên không có
                return a.getCompletionTimeSeconds() != null ? -1 : 1;
            } else {
                // OFFICIAL: sort theo completedAt (nộp sớm hơn = tốt hơn)
                if (a.getCompletedAt() == null && b.getCompletedAt() == null) {
                    return 0; // Cùng không có thời gian
                }
                if (a.getCompletedAt() == null) return 1; // Không có thời gian xếp dưới
                if (b.getCompletedAt() == null) return -1;
                return a.getCompletedAt().compareTo(b.getCompletedAt()); // Sớm hơn xếp trên
            }
        });

        // Assign ranks
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }

    @Transactional
    public void submitToContest(Long contestId, Long submissionId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // Check if user is registered/started
        Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                .findByContestId(contestId)
                .stream()
                .filter(reg -> reg.getUser().getId().equals(userId))
                .findFirst();
        
        if (registrationOpt.isEmpty()) {
            throw new RuntimeException("Bạn chưa đăng ký/khởi động contest này");
        }

        // Check if contest is ongoing based on type
        Instant now = Instant.now();
        if (contest.getContestType() == ContestType.PRACTICE) {
            ContestRegistrationEntity registration = registrationOpt.get();
            if (registration.getStartedAt() == null || registration.getEndedAt() == null) {
                throw new RuntimeException("Bạn chưa bắt đầu contest này");
            }
            if (now.isBefore(registration.getStartedAt()) || now.isAfter(registration.getEndedAt())) {
                throw new RuntimeException("Thời gian làm bài của bạn đã hết hoặc chưa bắt đầu");
            }
        } else if (contest.getContestType() == ContestType.OFFICIAL) {
            if (contest.getStartTime() == null || contest.getEndTime() == null) {
                throw new RuntimeException("Contest không có thời gian hợp lệ");
            }
            if (now.isBefore(contest.getStartTime()) || now.isAfter(contest.getEndTime())) {
                throw new RuntimeException("Contest chưa bắt đầu hoặc đã kết thúc");
            }
        }

        // Get submission
        SubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission không tồn tại"));

        // Check if submission belongs to user
        if (!submission.getUser().getId().equals(userId)) {
            throw new RuntimeException("Submission không thuộc về bạn");
        }

        // Check if problem is in contest
        Long problemId = submission.getProblem().getId();
        Optional<ContestProblemEntity> contestProblemOpt = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId);
        if (contestProblemOpt.isEmpty()) {
            throw new RuntimeException("Problem không thuộc contest này");
        }

        // Check if already submitted
        boolean alreadyExists = contestSubmissionRepository.findByContestId(contestId).stream()
                .anyMatch(cs -> cs.getSubmission().getId().equals(submissionId));
        
        if (alreadyExists) {
            throw new RuntimeException("Submission đã được gửi vào contest");
        }

        // Calculate score based on problem points and testcases passed
        ContestProblemEntity contestProblem = contestProblemOpt.get();
        Integer problemPoints = contestProblem.getPoints() != null ? contestProblem.getPoints() : 100;
        
        // Tính điểm dựa trên số testcase đúng
        Integer totalTestcases = submission.getTotalTestcases() != null && submission.getTotalTestcases() > 0 
                ? submission.getTotalTestcases() : 1;
        Integer totalCorrect = submission.getTotalCorrect() != null ? submission.getTotalCorrect() : 0;
        
        // Nếu totalCorrect/totalTestcases chưa có, thử parse từ statusMsg (ví dụ: "Wrong Answer (1/3)")
        if (totalCorrect == 0 && submission.getStatusMsg() != null) {
            String statusMsg = submission.getStatusMsg();
            // Pattern: "Wrong Answer (1/3)" hoặc "Accepted (3/3)"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((\\d+)/(\\d+)\\)");
            java.util.regex.Matcher matcher = pattern.matcher(statusMsg);
            if (matcher.find()) {
                try {
                    totalCorrect = Integer.parseInt(matcher.group(1));
                    totalTestcases = Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        // Tính điểm: (số testcase đúng / tổng số testcase) * điểm của bài
        // Sử dụng Math.round để làm tròn đúng (tránh mất điểm do integer division)
        Integer score = 0;
        if (totalTestcases > 0 && totalCorrect >= 0) {
            // Tính bằng double để có độ chính xác, sau đó làm tròn
            double scoreDouble = ((double) totalCorrect / (double) totalTestcases) * problemPoints;
            score = (int) Math.round(scoreDouble);
        }

        // Create contest submission
        ContestSubmissionEntity contestSubmission = ContestSubmissionEntity.builder()
                .contest(contest)
                .submission(submission)
                .score(score)
                .submittedAt(Instant.now())
                .build();

        contestSubmissionRepository.save(contestSubmission);
    }

    public List<ContestSubmissionResponse> getContestSubmissions(Long contestId, Long problemId, Long userId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        // Check if user is registered
        if (userId != null && !contestRegistrationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new RuntimeException("Bạn chưa đăng ký contest này");
        }

        List<ContestSubmissionEntity> contestSubmissions;
        if (problemId != null) {
            // Get submissions for specific problem
            contestSubmissions = contestSubmissionRepository.findByContestId(contestId).stream()
                    .filter(cs -> cs.getSubmission().getProblem().getId().equals(problemId))
                    .filter(cs -> userId == null || cs.getSubmission().getUser().getId().equals(userId))
                    .collect(Collectors.toList());
        } else {
            // Get all submissions for user in contest
            if (userId == null) {
                throw new RuntimeException("User ID là bắt buộc");
            }
            contestSubmissions = contestSubmissionRepository.findByContestIdAndUserId(contestId, userId);
        }

        // Với PRACTICE contest: chỉ lấy submissions của attempt hiện tại
        if (contest.getContestType() == ContestType.PRACTICE && userId != null) {
            Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                    .findByContestId(contestId)
                    .stream()
                    .filter(reg -> reg.getUser().getId().equals(userId))
                    .findFirst();
            
            if (registrationOpt.isPresent()) {
                ContestRegistrationEntity registration = registrationOpt.get();
                if (registration.getStartedAt() != null && registration.getEndedAt() != null) {
                    Instant currentStart = registration.getStartedAt();
                    Instant currentEnd = registration.getEndedAt();
                    
                    contestSubmissions = contestSubmissions.stream()
                            .filter(cs -> {
                                Instant subTime = cs.getSubmittedAt();
                                // Chỉ lấy submissions trong khoảng thời gian của attempt hiện tại
                                return (subTime.isAfter(currentStart) || subTime.equals(currentStart))
                                        && (subTime.isBefore(currentEnd) || subTime.equals(currentEnd));
                            })
                            .collect(Collectors.toList());
                } else {
                    // Chưa bắt đầu attempt → không có submissions
                    contestSubmissions = new ArrayList<>();
                }
            }
        }

        return contestSubmissions.stream()
                .map((ContestSubmissionEntity cs) -> {
                    SubmissionEntity submission = cs.getSubmission();
                    ProblemEntity problem = submission.getProblem();
                    ContestProblemEntity contestProblem = contestProblemRepository.findByContestIdAndProblemId(contestId, problem.getId())
                            .orElse(null);
                    
                    // Convert memoryKb to MB for display
                    Integer memoryMb = submission.getMemoryKb() != null ? submission.getMemoryKb() / 1024 : null;
                    // Parse runtime from statusRuntime string (e.g., "123ms" -> 123)
                    Integer runtimeMs = null;
                    if (submission.getStatusRuntime() != null && submission.getStatusRuntime().endsWith("ms")) {
                        try {
                            runtimeMs = Integer.parseInt(submission.getStatusRuntime().replace("ms", "").trim());
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                    
                    return ContestSubmissionResponse.builder()
                            .id(cs.getId())
                            .submissionId(submission.getId())
                            .problemId(problem.getId())
                            .problemTitle(problem.getTitle())
                            .problemOrder(contestProblem != null ? contestProblem.getProblemOrder() : "")
                            .score(cs.getScore())
                            .isAccepted(submission.getIsAccepted())
                            .language(submission.getLanguage() != null ? submission.getLanguage().getName() : "")
                            .submittedAt(cs.getSubmittedAt())
                            .statusMsg(submission.getStatusMsg())
                            .runtime(runtimeMs != null ? runtimeMs : 0)
                            .memory(memoryMb != null ? memoryMb : 0)
                            .codeContent(submission.getCodeContent())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<ContestRegistrationResponse> getContestRegistrations(Long contestId) {
        ContestEntity contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest không tồn tại"));

        if (contest.getIsDeleted()) {
            throw new RuntimeException("Contest đã bị xóa");
        }

        List<ContestRegistrationEntity> registrations = contestRegistrationRepository.findByContestId(contestId);
        
        return registrations.stream()
                .map(reg -> ContestRegistrationResponse.builder()
                        .userId(reg.getUser().getId())
                        .username(reg.getUser().getUsername())
                        .avatar(reg.getUser().getAvatar())
                        .registeredAt(reg.getRegisteredAt())
                        .build())
                .collect(Collectors.toList());
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

    private ContestProblemResponse mapToContestProblemResponse(ContestProblemEntity cp, Long contestId, Long userId) {
        ProblemEntity problem = cp.getProblem();
        boolean isSolved = false;
        Integer bestScore = 0;

        // CHỈ lấy từ contest submissions, KHÔNG dùng UserProblemBestEntity (trạng thái ở ProblemsPage)
        // Để tách biệt hoàn toàn: làm ở ProblemsPage ≠ làm trong Contest
        if (userId != null) {
            // Lấy contest để biết loại contest
            ContestEntity contest = contestRepository.findById(contestId).orElse(null);
            
            // Lấy registration để biết attempt hiện tại
            Optional<ContestRegistrationEntity> registrationOpt = contestRegistrationRepository
                    .findByContestId(contestId)
                    .stream()
                    .filter(reg -> reg.getUser().getId().equals(userId))
                    .findFirst();
            
            List<ContestSubmissionEntity> contestSubmissions = contestSubmissionRepository
                    .findByContestIdAndUserId(contestId, userId).stream()
                    .filter(cs -> cs.getSubmission().getProblem().getId().equals(problem.getId()))
                    .collect(Collectors.toList());
            
            // Với PRACTICE contest: chỉ lấy submissions của attempt hiện tại
            if (contest != null && contest.getContestType() == ContestType.PRACTICE && registrationOpt.isPresent()) {
                ContestRegistrationEntity registration = registrationOpt.get();
                if (registration.getStartedAt() != null && registration.getEndedAt() != null) {
                    Instant currentStart = registration.getStartedAt();
                    Instant currentEnd = registration.getEndedAt();
                    
                    contestSubmissions = contestSubmissions.stream()
                            .filter(cs -> {
                                Instant subTime = cs.getSubmittedAt();
                                // Chỉ lấy submissions trong khoảng thời gian của attempt hiện tại
                                return (subTime.isAfter(currentStart) || subTime.equals(currentStart))
                                        && (subTime.isBefore(currentEnd) || subTime.equals(currentEnd));
                            })
                            .collect(Collectors.toList());
                } else {
                    // Chưa bắt đầu attempt → không có submissions
                    contestSubmissions = new ArrayList<>();
                }
            }
            // Với OFFICIAL contest: giữ nguyên (lấy tất cả submissions)
            
            if (!contestSubmissions.isEmpty()) {
                // Tìm submission có score cao nhất trong attempt hiện tại
                // Sắp xếp theo score giảm dần, nếu cùng score thì ưu tiên submission sớm hơn
                ContestSubmissionEntity bestContestSubmission = contestSubmissions.stream()
                        .sorted((a, b) -> {
                            // Tính điểm cho a và b (có thể từ score hoặc parse từ statusMsg)
                            Integer scoreA = calculateScoreFromSubmission(a, cp.getPoints() != null ? cp.getPoints() : 100);
                            Integer scoreB = calculateScoreFromSubmission(b, cp.getPoints() != null ? cp.getPoints() : 100);
                            
                            // So sánh score: score cao hơn đứng trước (giảm dần)
                            int scoreCompare = Integer.compare(scoreB, scoreA);
                            if (scoreCompare != 0) return scoreCompare;
                            // Nếu cùng score, ưu tiên submission sớm hơn (tăng dần theo thời gian)
                            return a.getSubmittedAt().compareTo(b.getSubmittedAt());
                        })
                        .findFirst()  // Lấy submission đầu tiên sau khi sắp xếp (score cao nhất)
                        .orElse(null);
                
                if (bestContestSubmission != null) {
                    // Tính lại điểm từ submission (có thể từ score hoặc parse từ statusMsg)
                    bestScore = calculateScoreFromSubmission(bestContestSubmission, cp.getPoints() != null ? cp.getPoints() : 100);
                    isSolved = bestContestSubmission.getSubmission().getIsAccepted();
                }
            }
        }

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
                .isSolved(isSolved)
                .bestScore(bestScore)
                .build();
    }

    // Helper method để tính điểm từ submission (từ score hoặc parse từ statusMsg)
    private Integer calculateScoreFromSubmission(ContestSubmissionEntity cs, Integer problemPoints) {
        SubmissionEntity submission = cs.getSubmission();
        
        // Nếu submission đã được judge (có totalTestcases > 0), tính lại từ submission để đảm bảo chính xác
        // Vì ContestSubmissionEntity.score có thể chưa được cập nhật hoặc bị lỗi
        if (submission.getTotalTestcases() != null && submission.getTotalTestcases() > 0 
                && submission.getTotalCorrect() != null && submission.getTotalCorrect() >= 0) {
            // Tính điểm từ totalCorrect/totalTestcases (chính xác nhất)
            double scoreDouble = ((double) submission.getTotalCorrect() / (double) submission.getTotalTestcases()) * problemPoints;
            return (int) Math.round(scoreDouble);
        }
        
        // Nếu chưa có totalTestcases, thử parse từ statusMsg
        if (submission.getStatusMsg() != null) {
            String statusMsg = submission.getStatusMsg();
            // Pattern: "Wrong Answer (1/3)" hoặc "Accepted (3/3)"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((\\d+)/(\\d+)\\)");
            java.util.regex.Matcher matcher = pattern.matcher(statusMsg);
            if (matcher.find()) {
                try {
                    int totalCorrect = Integer.parseInt(matcher.group(1));
                    int totalTestcases = Integer.parseInt(matcher.group(2));
                    
                    if (totalTestcases > 0 && totalCorrect >= 0) {
                        // Tính điểm: (số testcase đúng / tổng số testcase) * điểm của bài
                        double scoreDouble = ((double) totalCorrect / (double) totalTestcases) * problemPoints;
                        return (int) Math.round(scoreDouble);
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        // Cuối cùng, nếu ContestSubmissionEntity.score đã được set, dùng nó (kể cả khi = 0)
        if (cs.getScore() != null) {
            return cs.getScore();
        }
        
        return 0;
    }
}

