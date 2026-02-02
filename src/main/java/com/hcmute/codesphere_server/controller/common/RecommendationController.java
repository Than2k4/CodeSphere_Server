package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.RecommendationResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller để xử lý recommendations
 * Gọi Python ML API để lấy recommendations cho users
 */
@Slf4j
@RestController
@RequestMapping("${base.url}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    /**
     * Lấy recommendations cho user hiện tại
     * 
     * @param limit Số lượng recommendations muốn lấy (mặc định 10)
     * @param useOpenAI Có dùng OpenAI để refine và thêm explanation không (mặc định false)
     * @param authentication Thông tin authentication của user
     * @return List các recommendations
     */
    @GetMapping
    public ResponseEntity<DataResponse<List<RecommendationResponse>>> getRecommendations(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) Boolean useOpenAI,
            Authentication authentication) {
        
        try {
            // Kiểm tra authentication
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrinciple)) {
                return ResponseEntity.status(401)
                    .body(DataResponse.<List<RecommendationResponse>>builder()
                        .status("error")
                        .message("Unauthorized - Vui lòng đăng nhập")
                        .data(null)
                        .build());
            }
            
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            log.info("Đang lấy recommendations cho user {}", userId);
            
            // Kiểm tra Python API có đang chạy không
            if (!recommendationService.isApiAvailable()) {
                log.warn("Python ML API không khả dụng");
                return ResponseEntity.status(503)
                    .body(DataResponse.<List<RecommendationResponse>>builder()
                        .status("error")
                        .message("Recommendation service tạm thời không khả dụng. Vui lòng thử lại sau.")
                        .data(null)
                        .build());
            }
            
            // Lấy recommendations (mặc định dùng OpenAI để có explanation, user có thể tắt)
            List<RecommendationResponse> recommendations = recommendationService.getRecommendations(
                userId, 
                limit, 
                useOpenAI != null ? useOpenAI : true
            );
            
            if (recommendations.isEmpty()) {
                log.info("Không có recommendations cho user {}", userId);
                return ResponseEntity.ok(DataResponse.<List<RecommendationResponse>>builder()
                    .status("success")
                    .message("Không có recommendations phù hợp")
                    .data(recommendations)
                    .build());
            }
            
            log.info("Đã lấy được {} recommendations cho user {}", recommendations.size(), userId);
            
            return ResponseEntity.ok(DataResponse.<List<RecommendationResponse>>builder()
                .status("success")
                .message("Lấy recommendations thành công")
                .data(recommendations)
                .build());
                
        } catch (Exception e) {
            log.error("Lỗi khi lấy recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(DataResponse.<List<RecommendationResponse>>builder()
                    .status("error")
                    .message("Lỗi khi lấy recommendations: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
    
    /**
     * Health check endpoint để kiểm tra Python ML API
     */
    @GetMapping("/health")
    public ResponseEntity<DataResponse<Boolean>> checkHealth() {
        boolean isAvailable = recommendationService.isApiAvailable();
        
        return ResponseEntity.ok(DataResponse.<Boolean>builder()
            .status(isAvailable ? "success" : "error")
            .message(isAvailable ? "Python ML API đang hoạt động" : "Python ML API không khả dụng")
            .data(isAvailable)
            .build());
    }
}

