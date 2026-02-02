package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.GlobalLeaderboardResponse;
import com.hcmute.codesphere_server.model.payload.response.LeaderboardResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Lấy bảng xếp hạng của một problem
     * GET /api/v1/leaderboard?problemId=1
     */
    @GetMapping
    public ResponseEntity<DataResponse<List<LeaderboardResponse>>> getLeaderboard(
            @RequestParam(required = false) Long problemId) {
        
        if (problemId == null) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error("problemId là bắt buộc"));
        }

        try {
            List<LeaderboardResponse> leaderboard = leaderboardService.getLeaderboard(problemId);
            return ResponseEntity.ok(DataResponse.success(leaderboard));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    /**
     * Lấy xếp hạng của user hiện tại trong một problem
     * GET /api/v1/leaderboard/my-rank?problemId=1
     */
    @GetMapping("/my-rank")
    public ResponseEntity<DataResponse<LeaderboardResponse>> getMyRank(
            @RequestParam(required = false) Long problemId,
            Authentication authentication) {
        
        if (problemId == null) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error("problemId là bắt buộc"));
        }

        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            LeaderboardResponse myRank = leaderboardService.getMyRank(problemId, userId);
            
            if (myRank == null) {
                DataResponse<LeaderboardResponse> response = DataResponse.success(null);
                response.setMessage("Bạn chưa nộp bài nào cho bài tập này");
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(DataResponse.success(myRank));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    /**
     * Lấy global leaderboard - xếp hạng tất cả users theo tổng số bài đã giải đúng
     * GET /api/v1/leaderboard/global
     */
    @GetMapping("/global")
    public ResponseEntity<DataResponse<List<GlobalLeaderboardResponse>>> getGlobalLeaderboard() {
        try {
            List<GlobalLeaderboardResponse> leaderboard = leaderboardService.getGlobalLeaderboard();
            return ResponseEntity.ok(DataResponse.success(leaderboard));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

