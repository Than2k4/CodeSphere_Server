package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyStatsResponse {
    
    // Tổng số bài đã giải đúng
    private Integer totalSolved;
    
    // Số bài đã giải theo độ khó
    private Integer solvedEasy;
    private Integer solvedMedium;
    private Integer solvedHard;
    
    // Tổng số bài đã thử (có ít nhất 1 submission)
    private Integer totalAttempted;
    
    // Tổng số submission
    private Integer totalSubmissions;
    
    // Tổng số submission đúng
    private Integer acceptedSubmissions;
    
    // Tỷ lệ chấp nhận (acceptance rate)
    private Double acceptanceRate;
    
    // Tiến độ (số bài đã giải / số bài đã thử)
    private Double progressRate;
    
    // Số bài đã thử theo độ khó
    private Integer attemptedEasy;
    private Integer attemptedMedium;
    private Integer attemptedHard;
}

