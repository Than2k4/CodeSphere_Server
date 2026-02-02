package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.MyStatsResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemStatsResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * GET /api/v1/stats/my-stats
     * Thống kê cá nhân của user hiện tại
     * Bao gồm: số bài đúng, độ khó đã làm, tiến độ
     */
    @GetMapping("/my-stats")
    public ResponseEntity<DataResponse<MyStatsResponse>> getMyStats(
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            MyStatsResponse stats = statisticsService.getMyStats(userId);
            return ResponseEntity.ok(DataResponse.success(stats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/stats/problems/{problemId}
     * Thống kê của một bài tập cụ thể
     * Bao gồm: tổng số submission, số submission đúng, tỷ lệ chấp nhận, số user đã thử/giải
     */
    @GetMapping("/problems/{problemId}")
    public ResponseEntity<DataResponse<ProblemStatsResponse>> getProblemStats(
            @PathVariable Long problemId) {
        
        try {
            ProblemStatsResponse stats = statisticsService.getProblemStats(problemId);
            return ResponseEntity.ok(DataResponse.success(stats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

