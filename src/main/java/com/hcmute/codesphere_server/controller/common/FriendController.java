package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.SendFriendRequestRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.FriendRequestResponse;
import com.hcmute.codesphere_server.model.payload.response.FriendResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request")
    public ResponseEntity<DataResponse<FriendRequestResponse>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            FriendRequestResponse friendRequest = friendService.sendFriendRequest(request, userId);
            return ResponseEntity.ok(DataResponse.success(friendRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<DataResponse<List<FriendRequestResponse>>> getFriendRequests(
            @RequestParam(required = false, defaultValue = "all") String type, // sent, received, all
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            List<FriendRequestResponse> requests = friendService.getFriendRequests(userId, type);
            return ResponseEntity.ok(DataResponse.success(requests));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<DataResponse<FriendRequestResponse>> acceptFriendRequest(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            FriendRequestResponse request = friendService.acceptFriendRequest(id, userId);
            return ResponseEntity.ok(DataResponse.success(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<DataResponse<FriendRequestResponse>> rejectFriendRequest(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            FriendRequestResponse request = friendService.rejectFriendRequest(id, userId);
            return ResponseEntity.ok(DataResponse.success(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<DataResponse<String>> cancelFriendRequest(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            friendService.cancelFriendRequest(id, userId);
            return ResponseEntity.ok(DataResponse.success("Đã hủy lời mời kết bạn"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<DataResponse<List<FriendResponse>>> getFriends(
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            List<FriendResponse> friends = friendService.getFriends(userId);
            return ResponseEntity.ok(DataResponse.success(friends));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<DataResponse<String>> unfriend(
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long currentUserId = Long.parseLong(userPrinciple.getUserId());
            
            friendService.unfriend(userId, currentUserId);
            return ResponseEntity.ok(DataResponse.success("Đã hủy kết bạn"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

