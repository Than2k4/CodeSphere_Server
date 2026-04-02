package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long totalProblems;
    private Long totalContests;
    private Long totalSubmissions;
    private Long activeUsers; // Last 30 days
    private Long activeNow; // Active in last 24 hours
    private Long blockedUsers;
    private Long administrators;
    private Long newUsersThisMonth;
    private Long submissionsToday;

    // Moderation KPI - Daily/Weekly
    private Long newPostsToday;
    private Long newPostsThisWeek;
    private Long violationReportsToday;
    private Long violationReportsThisWeek;
    private Long moderationHandledToday;
    private Long moderationHandledThisWeek;
    private Double avgModerationHoursToday;
    private Double avgModerationHoursThisWeek;
    private Double restoreRateThisWeek;

    // Alert flags for unusual spikes
    private Boolean spamSpikeAlert;
    private Boolean postingSpikeAlert;
    private String alertMessage;

    // Learning KPI trend chart
    private List<SubmissionTrendPointResponse> submissionTrend;
}

