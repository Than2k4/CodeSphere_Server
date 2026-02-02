package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.NotificationResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<DataResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<NotificationResponse> notifications = notificationService.getNotifications(userId, type, isRead, pageable);
            return ResponseEntity.ok(DataResponse.success(notifications));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<DataResponse<Long>> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            Long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(DataResponse.success(count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<DataResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            NotificationResponse notification = notificationService.markAsRead(id, userId);
            return ResponseEntity.ok(DataResponse.success(notification));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<DataResponse<String>> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(DataResponse.success("Đã đánh dấu tất cả thông báo đã đọc"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            notificationService.deleteNotification(id, userId);
            return ResponseEntity.ok(DataResponse.success("Đã xóa thông báo"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

