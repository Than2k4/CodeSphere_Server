package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.FollowResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<DataResponse<String>> followUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long currentUserId = Long.parseLong(userPrinciple.getUserId());
            
            // Lấy thông tin follower để gửi notification
            var follower = followService.getFollowerInfo(currentUserId);
            
            followService.followUser(currentUserId, userId);
            
            // Gửi notification sau khi follow thành công (ngoài transaction)
            if (follower != null) {
                followService.sendFollowNotification(userId, currentUserId, follower);
            }
            
            return ResponseEntity.ok(DataResponse.success("Đã follow user"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<DataResponse<String>> unfollowUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long currentUserId = Long.parseLong(userPrinciple.getUserId());
            
            followService.unfollowUser(currentUserId, userId);
            return ResponseEntity.ok(DataResponse.success("Đã unfollow user"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<DataResponse<List<FollowResponse>>> getFollowers(
            @PathVariable Long userId) {
        
        try {
            List<FollowResponse> followers = followService.getFollowers(userId);
            return ResponseEntity.ok(DataResponse.success(followers));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<DataResponse<List<FollowResponse>>> getFollowing(
            @PathVariable Long userId) {
        
        try {
            List<FollowResponse> following = followService.getFollowing(userId);
            return ResponseEntity.ok(DataResponse.success(following));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

