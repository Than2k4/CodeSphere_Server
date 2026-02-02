package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.ProblemBookmarkService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/problems/{problemId}/bookmark")
@RequiredArgsConstructor
public class ProblemBookmarkController {

    private final ProblemBookmarkService problemBookmarkService;

    /**
     * POST /api/v1/problems/{problemId}/bookmark
     * Toggle bookmark (thêm hoặc xóa bookmark)
     */
    @PostMapping
    public ResponseEntity<DataResponse<BookmarkResponse>> toggleBookmark(
            @PathVariable Long problemId,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            boolean isBookmarked = problemBookmarkService.toggleBookmark(userId, problemId);
            
            BookmarkResponse response = BookmarkResponse.builder()
                    .problemId(problemId)
                    .isBookmarked(isBookmarked)
                    .message(isBookmarked ? "Đã đánh dấu sao bài tập" : "Đã bỏ đánh dấu sao bài tập")
                    .build();
            
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/problems/{problemId}/bookmark
     * Kiểm tra user đã bookmark problem chưa
     */
    @GetMapping
    public ResponseEntity<DataResponse<BookmarkResponse>> checkBookmark(
            @PathVariable Long problemId,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            boolean isBookmarked = problemBookmarkService.isBookmarked(userId, problemId);
            
            BookmarkResponse response = BookmarkResponse.builder()
                    .problemId(problemId)
                    .isBookmarked(isBookmarked)
                    .message(isBookmarked ? "Đã đánh dấu sao" : "Chưa đánh dấu sao")
                    .build();
            
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BookmarkResponse {
        private Long problemId;
        private Boolean isBookmarked;
        private String message;
    }
}

