package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateTagRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminTagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${base.url}/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final AdminTagService adminTagService;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private String getActorRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "UNKNOWN";
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("UNKNOWN");
    }

    @PostMapping
    public ResponseEntity<DataResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            TagResponse response = adminTagService.createTag(
                    request,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteTag(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminTagService.deleteTag(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Tag deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
        }
    }
}

