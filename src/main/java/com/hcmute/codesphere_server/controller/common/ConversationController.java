package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.AddMemberRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateConversationRequest;
import com.hcmute.codesphere_server.model.payload.request.TransferAdminRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateConversationRequest;
import com.hcmute.codesphere_server.model.payload.response.ConversationResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<DataResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            ConversationResponse conversation = conversationService.createConversation(request, userId);
            return ResponseEntity.ok(DataResponse.success(conversation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<DataResponse<List<ConversationResponse>>> getConversations(
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            List<ConversationResponse> conversations = conversationService.getConversations(userId);
            return ResponseEntity.ok(DataResponse.success(conversations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ConversationResponse>> getConversationById(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            ConversationResponse conversation = conversationService.getConversationById(id, userId);
            return ResponseEntity.ok(DataResponse.success(conversation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<ConversationResponse>> updateConversation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            ConversationResponse conversation = conversationService.updateConversation(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(conversation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<DataResponse<ConversationResponse>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            ConversationResponse conversation = conversationService.addMember(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(conversation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<DataResponse<String>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long currentUserId = Long.parseLong(userPrinciple.getUserId());
            
            conversationService.removeMember(id, userId, currentUserId);
            return ResponseEntity.ok(DataResponse.success("Đã xóa thành viên khỏi conversation"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<DataResponse<String>> leaveGroup(
            @PathVariable Long id,
            @RequestBody(required = false) TransferAdminRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long currentUserId = Long.parseLong(userPrinciple.getUserId());
            
            // Rời nhóm với transfer admin nếu cần
            Long newAdminId = request != null ? request.getNewAdminId() : null;
            conversationService.leaveGroupWithTransfer(id, currentUserId, newAdminId);
            return ResponseEntity.ok(DataResponse.success("Đã rời nhóm"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/transfer-admin")
    public ResponseEntity<DataResponse<ConversationResponse>> transferAdmin(
            @PathVariable Long id,
            @Valid @RequestBody TransferAdminRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            ConversationResponse conversation = conversationService.transferAdmin(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(conversation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

