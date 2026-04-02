package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.UserManagementResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

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
                .map(a -> a.getAuthority())
                .orElse("UNKNOWN");
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<UserManagementResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserManagementResponse> users = adminUserService.getUsers(pageable, search, role, status);
            return ResponseEntity.ok(DataResponse.success(users));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<UserManagementResponse>> getUserById(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserManagementResponse user = adminUserService.getUserById(id);
            return ResponseEntity.ok(DataResponse.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DataResponse<UserManagementResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            UserManagementResponse user = adminUserService.updateUserStatus(
                    id,
                    status,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<DataResponse<UserManagementResponse>> blockUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            UserManagementResponse user = adminUserService.blockUser(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<DataResponse<UserManagementResponse>> unblockUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            UserManagementResponse user = adminUserService.unblockUser(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<DataResponse<UserManagementResponse>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            UserManagementResponse user = adminUserService.changeUserRole(
                    id,
                    role,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminUserService.deleteUser(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Xóa user thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

