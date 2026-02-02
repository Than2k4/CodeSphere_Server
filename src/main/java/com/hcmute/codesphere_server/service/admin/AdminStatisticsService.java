package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.payload.response.DashboardStatsResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final ContestRepository contestRepository;
    private final AccountRepository accountRepository;

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
                .build();
    }
}

