package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestLeaderboardResponse {
    private Integer rank;
    private Long userId;
    private String username;
    private Integer totalScore;
    private Map<String, Integer> problemScores; // Key: problem order (A, B, C...), Value: score
    private Instant completedAt; // Thời gian hoàn thành contest (khi giải hết tất cả bài hoặc hết thời gian)
    private Long completionTimeSeconds; // Thời gian làm bài tính bằng giây (endedAt - startedAt), chỉ có khi đã hoàn thành
    private Instant lastSubmissionTime; // Thời gian submission cuối cùng (deprecated, dùng completedAt)
    private Integer totalSubmissions; // Số lần submission (deprecated, không hiển thị nữa)
}

