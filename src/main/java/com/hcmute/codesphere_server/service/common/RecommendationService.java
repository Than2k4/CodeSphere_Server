package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.payload.response.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service để gọi Python ML API và lấy recommendations
 * Python API chạy tại http://localhost:8000
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    
    private final RestTemplate restTemplate;
    private final ProblemService problemService;
    
    @Value("${ml.recommendation.api.url:http://localhost:8000}")
    private String mlApiUrl;
    
    /**
     * Lấy recommendations cho một user từ Python ML API
     * 
     * @param userId ID của user
     * @param limit Số lượng recommendations muốn lấy (mặc định 10)
     * @param useOpenAI Có dùng OpenAI để refine và thêm explanation không
     * @return List các recommendations
     */
    public List<RecommendationResponse> getRecommendations(Long userId, Integer limit, Boolean useOpenAI) {
        int maxRetries = 2; // Retry tối đa 2 lần
        int retryCount = 0;
        
        while (retryCount <= maxRetries) {
            try {
                log.info("Đang lấy recommendations cho user {} từ Python API (useOpenAI: {}, retry: {})", 
                    userId, useOpenAI, retryCount);
                
                // Gọi Python API
                String url = mlApiUrl + "/recommendations/" + userId + "?limit=" + limit + "&use_openai=" + (useOpenAI != null && useOpenAI);
                //http://localhost:8000/recommendations/{userId}?limit=10&use_openai=true
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    log.warn("Python API trả về null cho user {}", userId);
                    return new ArrayList<>();
                }
                
                // Parse response từ Python API
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recommendations = (List<Map<String, Object>>) body.get("recommendations");
                
                if (recommendations == null || recommendations.isEmpty()) {
                    log.info("Không có recommendations cho user {}", userId);
                    return new ArrayList<>();
                }
                
                // Chuyển đổi sang RecommendationResponse và lấy thông tin problem
                List<RecommendationResponse> result = new ArrayList<>();
                
                for (Map<String, Object> rec : recommendations) {
                    Long problemId = Long.valueOf(rec.get("problem_id").toString());
                    Double predictedRating = Double.valueOf(rec.get("predicted_rating").toString());
                    
                    // Lấy explanation từ response nếu có (từ OpenAI)
                    String explanation = null;
                    if (rec.containsKey("explanation")) {
                        explanation = rec.get("explanation").toString();
                    }
                    
                    // Lấy thông tin problem từ ProblemService
                    try {
                        var problemDetail = problemService.getProblemById(problemId);
                        if (problemDetail != null) {
                            RecommendationResponse recommendation = RecommendationResponse.builder()
                                .problemId(problemId)
                                .predictedRating(predictedRating)
                                .title(problemDetail.getTitle())
                                .level(problemDetail.getLevel())
                                .explanation(explanation)
                                .build();
                            result.add(recommendation);
                        }
                    } catch (Exception e) {
                        log.warn("Không thể lấy thông tin problem {}: {}", problemId, e.getMessage());
                        // Vẫn thêm recommendation nhưng không có title
                        RecommendationResponse recommendation = RecommendationResponse.builder()
                            .problemId(problemId)
                            .predictedRating(predictedRating)
                            .explanation(explanation)
                            .build();
                        result.add(recommendation);
                    }
                }
                
                log.info("Đã lấy được {} recommendations cho user {}", result.size(), userId);
                return result;
                
            } catch (RestClientException e) {
                retryCount++;
                String errorMsg = e.getMessage();
                
                // Nếu là timeout và chưa retry hết, thử lại
                if (errorMsg != null && errorMsg.contains("Read timed out") && retryCount <= maxRetries) {
                    log.warn("Timeout khi gọi Python ML API cho user {} (retry {}/{}), thử lại...", 
                        userId, retryCount, maxRetries);
                    
                    // Nếu đang dùng OpenAI, thử lại với useOpenAI=false (nhanh hơn)
                    if (useOpenAI != null && useOpenAI && retryCount == maxRetries) {
                        log.info("Thử lại với useOpenAI=false để tránh timeout");
                        return getRecommendations(userId, limit, false);
                    }
                    
                    // Đợi 1 giây trước khi retry
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue; // Retry
                }
                
                // Nếu không phải timeout hoặc đã retry hết
                log.error("Lỗi khi gọi Python ML API cho user {} (sau {} retries): {}", 
                    userId, retryCount, errorMsg);
                
                // Nếu đang dùng OpenAI, thử fallback về local model
                if (useOpenAI != null && useOpenAI && retryCount > maxRetries) {
                    log.info("Fallback: Thử lại với useOpenAI=false");
                    try {
                        return getRecommendations(userId, limit, false);
                    } catch (Exception fallbackError) {
                        log.error("Fallback cũng thất bại: {}", fallbackError.getMessage());
                    }
                }
                
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("Lỗi không mong đợi khi lấy recommendations cho user {}: {}", 
                    userId, e.getMessage(), e);
                return new ArrayList<>();
            }
        }
        
        // Nếu retry hết mà vẫn lỗi
        log.error("Không thể lấy recommendations cho user {} sau {} retries", userId, maxRetries);
        return new ArrayList<>();
    }
    
    /**
     * Kiểm tra Python ML API có đang chạy không
     * 
     * @return true nếu API đang chạy, false nếu không
     */
    public boolean isApiAvailable() {
        try {
            String url = mlApiUrl + "/health";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Python ML API không khả dụng: {}", e.getMessage());
            return false;
        }
    }
}

