package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateTagRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${base.url}/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final AdminTagService adminTagService;

    @PostMapping
    public ResponseEntity<DataResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        // Kiểm tra quyền admin
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            TagResponse response = adminTagService.createTag(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

