package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.payload.response.DashboardStatsResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionTrendPointResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final ContestRepository contestRepository;
    private final AccountRepository accountRepository;
        private final PostRepository postRepository;
        private final PostReportRepository postReportRepository;

    public DashboardStatsResponse getDashboardStats() {
        // Total users (not deleted)
        Specification<com.hcmute.codesphere_server.model.entity.UserEntity> userSpec = (root, query, cb) ->
                cb.equal(root.get("isDeleted"), false);
        Long totalUsers = userRepository.count(userSpec);

        // Total problems (active)
        Specification<com.hcmute.codesphere_server.model.entity.ProblemEntity> problemSpec = (root, query, cb) ->
                cb.equal(root.get("status"), true);
        Long totalProblems = problemRepository.count(problemSpec);

        // Total contests (not deleted)
        Long totalContests = contestRepository.count(
                (root, query, cb) -> cb.equal(root.get("isDeleted"), false)
        );

        // Total submissions (not deleted)
        Long totalSubmissions = submissionRepository.count(
                (root, query, cb) -> cb.equal(root.get("isDeleted"), false)
        );

        // Active users (last 30 days)
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Specification<com.hcmute.codesphere_server.model.entity.UserEntity> activeUserSpec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.greaterThanOrEqualTo(root.get("lastOnline"), thirtyDaysAgo)
                );
        Long activeUsers = userRepository.count(activeUserSpec);

        // Active now (last 24 hours)
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        Specification<com.hcmute.codesphere_server.model.entity.UserEntity> activeNowSpec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.greaterThanOrEqualTo(root.get("lastOnline"), twentyFourHoursAgo)
                );
        Long activeNow = userRepository.count(activeNowSpec);

        // Blocked users
        Long blockedUsers = accountRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.equal(root.get("isBlocked"), true)
                )
        );

        // Administrators (users with ROLE_ADMIN)
        Long administrators = accountRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.equal(root.join("role").get("name"), "ROLE_ADMIN")
                )
        );

        // New users this month
        java.time.LocalDate firstDayOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        Instant startOfMonth = firstDayOfMonth.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Specification<com.hcmute.codesphere_server.model.entity.UserEntity> newUsersSpec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startOfMonth)
                );
        Long newUsersThisMonth = userRepository.count(newUsersSpec);

        // Submissions today
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Long submissionsToday = submissionRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("isDeleted"), false),
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startOfToday)
                )
        );

        // Moderation KPI window boundaries
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfTodayKpi = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
        Instant startOfTomorrowKpi = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant();
        Instant startOfWeekKpi = LocalDate.now(zoneId).minusDays(6).atStartOfDay(zoneId).toInstant();

        Long newPostsToday = countNewPostsBetween(startOfTodayKpi, startOfTomorrowKpi);
        Long newPostsThisWeek = countNewPostsBetween(startOfWeekKpi, startOfTomorrowKpi);

        Long violationReportsToday = postReportRepository.countByCreatedAtBetween(startOfTodayKpi, startOfTomorrowKpi);
        Long violationReportsThisWeek = postReportRepository.countByCreatedAtBetween(startOfWeekKpi, startOfTomorrowKpi);

        Long moderationHandledToday = countModerationHandledBetween(startOfTodayKpi, startOfTomorrowKpi);
        Long moderationHandledThisWeek = countModerationHandledBetween(startOfWeekKpi, startOfTomorrowKpi);

        Double avgModerationHoursToday = calculateAverageModerationHours(startOfTodayKpi, startOfTomorrowKpi);
        Double avgModerationHoursThisWeek = calculateAverageModerationHours(startOfWeekKpi, startOfTomorrowKpi);

        Long restoredThisWeek = countRestoredBetween(startOfWeekKpi, startOfTomorrowKpi);
        Double restoreRateThisWeek = moderationHandledThisWeek > 0
                ? roundTo2Decimal((restoredThisWeek.doubleValue() / moderationHandledThisWeek.doubleValue()) * 100.0)
                : 0.0;

        double avgReportsPast7Days = calculateAverageDailyReports(7, zoneId);
        double avgPostsPast7Days = calculateAverageDailyPosts(7, zoneId);
        boolean spamSpikeAlert = violationReportsToday >= 10 && violationReportsToday > Math.max(5.0, avgReportsPast7Days * 2.0);
        boolean postingSpikeAlert = newPostsToday >= 25 && newPostsToday > Math.max(10.0, avgPostsPast7Days * 2.2);

        String alertMessage = null;
        if (spamSpikeAlert && postingSpikeAlert) {
            alertMessage = "Canh bao: Tang dot bien bao cao vi pham va luong dang bai viet.";
        } else if (spamSpikeAlert) {
            alertMessage = "Canh bao: So luong bao cao vi pham hom nay tang dot bien.";
        } else if (postingSpikeAlert) {
            alertMessage = "Canh bao: Luong dang bai viet hom nay tang dot bien.";
        }

                List<SubmissionTrendPointResponse> submissionTrend = buildSubmissionTrend(14, zoneId);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProblems(totalProblems)
                .totalContests(totalContests)
                .totalSubmissions(totalSubmissions)
                .activeUsers(activeUsers)
                .activeNow(activeNow)
                .blockedUsers(blockedUsers)
                .administrators(administrators)
                .newUsersThisMonth(newUsersThisMonth)
                .submissionsToday(submissionsToday)
                                .newPostsToday(newPostsToday)
                                .newPostsThisWeek(newPostsThisWeek)
                                .violationReportsToday(violationReportsToday)
                                .violationReportsThisWeek(violationReportsThisWeek)
                                .moderationHandledToday(moderationHandledToday)
                                .moderationHandledThisWeek(moderationHandledThisWeek)
                                .avgModerationHoursToday(avgModerationHoursToday)
                                .avgModerationHoursThisWeek(avgModerationHoursThisWeek)
                                .restoreRateThisWeek(restoreRateThisWeek)
                                .spamSpikeAlert(spamSpikeAlert)
                                .postingSpikeAlert(postingSpikeAlert)
                                .alertMessage(alertMessage)
                                .submissionTrend(submissionTrend)
                .build();
    }

        private List<SubmissionTrendPointResponse> buildSubmissionTrend(int days, ZoneId zoneId) {
                List<SubmissionTrendPointResponse> points = new ArrayList<>();

                for (int i = days - 1; i >= 0; i--) {
                        LocalDate date = LocalDate.now(zoneId).minusDays(i);
                        Instant from = date.atStartOfDay(zoneId).toInstant();
                        Instant to = date.plusDays(1).atStartOfDay(zoneId).toInstant();

                        long submissions = countSubmissionsBetween(from, to, false);
                        long accepted = countSubmissionsBetween(from, to, true);
                        double accuracy = submissions > 0 ? roundTo2Decimal((accepted * 100.0) / submissions) : 0.0;

                        points.add(SubmissionTrendPointResponse.builder()
                                        .date(date.toString())
                                        .submissions(submissions)
                                        .accepted(accepted)
                                        .accuracyRate(accuracy)
                                        .build());
                }

                return points;
        }

        private Long countNewPostsBetween(Instant fromTime, Instant toTime) {
                Specification<com.hcmute.codesphere_server.model.entity.PostEntity> spec = (root, query, cb) -> cb.and(
                                cb.equal(root.get("isDeleted"), false),
                                cb.greaterThanOrEqualTo(root.get("createdAt"), fromTime),
                                cb.lessThan(root.get("createdAt"), toTime)
                );
                return postRepository.count(spec);
        }

        private Long countModerationHandledBetween(Instant fromTime, Instant toTime) {
                Specification<com.hcmute.codesphere_server.model.entity.PostEntity> spec = (root, query, cb) -> cb.and(
                                cb.isNotNull(root.get("moderatedAt")),
                                cb.greaterThanOrEqualTo(root.get("moderatedAt"), fromTime),
                                cb.lessThan(root.get("moderatedAt"), toTime)
                );
                return postRepository.count(spec);
        }

        private Long countSubmissionsBetween(Instant fromTime, Instant toTime, boolean acceptedOnly) {
                Specification<com.hcmute.codesphere_server.model.entity.SubmissionEntity> spec = (root, query, cb) -> {
                        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                        predicates.add(cb.equal(root.get("isDeleted"), false));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
                        predicates.add(cb.lessThan(root.get("createdAt"), toTime));
                        if (acceptedOnly) {
                                predicates.add(cb.equal(root.get("isAccepted"), true));
                        }
                        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                };
                return submissionRepository.count(spec);
        }

        private Long countRestoredBetween(Instant fromTime, Instant toTime) {
                Specification<com.hcmute.codesphere_server.model.entity.PostEntity> spec = (root, query, cb) -> cb.and(
                                cb.equal(root.get("isDeleted"), false),
                                cb.equal(root.get("isBlocked"), false),
                                cb.isNotNull(root.get("moderationReasonCode")),
                                cb.isNotNull(root.get("moderatedAt")),
                                cb.greaterThanOrEqualTo(root.get("moderatedAt"), fromTime),
                                cb.lessThan(root.get("moderatedAt"), toTime)
                );
                return postRepository.count(spec);
        }

        private Double calculateAverageModerationHours(Instant fromTime, Instant toTime) {
                var moderatedPosts = postRepository.findAll((root, query, cb) -> cb.and(
                                cb.isNotNull(root.get("moderatedAt")),
                                cb.greaterThanOrEqualTo(root.get("moderatedAt"), fromTime),
                                cb.lessThan(root.get("moderatedAt"), toTime)
                ));

                if (moderatedPosts.isEmpty()) {
                        return 0.0;
                }

                double totalHours = moderatedPosts.stream()
                                .filter(p -> p.getCreatedAt() != null && p.getModeratedAt() != null)
                                .mapToDouble(p -> {
                                        long minutes = ChronoUnit.MINUTES.between(p.getCreatedAt(), p.getModeratedAt());
                                        return Math.max(0, minutes) / 60.0;
                                })
                                .sum();

                return roundTo2Decimal(totalHours / moderatedPosts.size());
        }

        private double calculateAverageDailyReports(int days, ZoneId zoneId) {
                long sum = 0;
                for (int i = 1; i <= days; i++) {
                        Instant from = LocalDate.now(zoneId).minusDays(i).atStartOfDay(zoneId).toInstant();
                        Instant to = LocalDate.now(zoneId).minusDays(i - 1L).atStartOfDay(zoneId).toInstant();
                        sum += postReportRepository.countByCreatedAtBetween(from, to);
                }
                return days > 0 ? (double) sum / days : 0.0;
        }

        private double calculateAverageDailyPosts(int days, ZoneId zoneId) {
                long sum = 0;
                for (int i = 1; i <= days; i++) {
                        Instant from = LocalDate.now(zoneId).minusDays(i).atStartOfDay(zoneId).toInstant();
                        Instant to = LocalDate.now(zoneId).minusDays(i - 1L).atStartOfDay(zoneId).toInstant();
                        sum += countNewPostsBetween(from, to);
                }
                return days > 0 ? (double) sum / days : 0.0;
        }

        private Double roundTo2Decimal(double value) {
                return Math.round(value * 100.0) / 100.0;
        }
}

