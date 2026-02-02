package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    
    private Integer rank; // Thứ hạng (1, 2, 3, ...)
    
    private Long userId;
    
    private String username;
    
    private Integer bestScore; // Điểm cao nhất của user cho bài này
    
    private Long bestSubmissionId; // ID của submission có điểm cao nhất
    
    private Integer totalSubmissions; // Tổng số lần nộp bài
    
    private Instant bestSubmissionTime; // Thời gian nộp bài điểm cao nhất
    
    private String statusMsg; // Status message của best submission
    
    private String statusRuntime; // Runtime của best submission
    
    private String statusMemory; // Memory của best submission
    
    private Boolean isAccepted; // Best submission có accepted không
}

