package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemStatsResponse {
    
    // ID của bài tập
    private Long problemId;
    
    // Tiêu đề bài tập
    private String problemTitle;
    
    // Mã bài tập
    private String problemCode;
    
    // Độ khó
    private String level;
    
    // Tổng số submission
    private Long totalSubmissions;
    
    // Tổng số submission đúng
    private Long acceptedSubmissions;
    
    // Tỷ lệ chấp nhận (acceptance rate)
    private Double acceptanceRate;
    
    // Tổng số user đã thử
    private Long totalUsersAttempted;
    
    // Tổng số user đã giải đúng
    private Long totalUsersSolved;
    
    // Tỷ lệ user giải đúng
    private Double solveRate;
}

