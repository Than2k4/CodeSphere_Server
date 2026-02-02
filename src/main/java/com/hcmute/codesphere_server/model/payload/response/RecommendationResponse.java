package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho recommendation
 * Chứa thông tin problem được recommend và điểm dự đoán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    
    /**
     * ID của problem được recommend
     */
    private Long problemId;
    
    /**
     * Điểm dự đoán từ model (0-5)
     * Điểm càng cao = user càng có khả năng thích problem này
     */
    private Double predictedRating;
    
    /**
     * Tiêu đề của problem (optional, có thể lấy từ ProblemService)
     */
    private String title;
    
    /**
     * Level của problem (EASY, MEDIUM, HARD)
     */
    private String level;
    
    /**
     * Explanation tại sao recommend bài này (từ OpenAI, nếu có)
     */
    private String explanation;
}

