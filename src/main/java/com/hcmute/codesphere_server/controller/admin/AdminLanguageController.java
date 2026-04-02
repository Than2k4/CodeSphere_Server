package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminLanguageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/languages")
@RequiredArgsConstructor
public class AdminLanguageController {

    private final AdminLanguageService adminLanguageService;

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
    public ResponseEntity<DataResponse<LanguageResponse>> createLanguage(
            @Valid @RequestBody CreateLanguageRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            LanguageResponse response = adminLanguageService.createLanguage(
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

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<LanguageResponse>> updateLanguage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLanguageRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            LanguageResponse response = adminLanguageService.updateLanguage(
                    id,
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
    public ResponseEntity<DataResponse<String>> deleteLanguage(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminLanguageService.deleteLanguage(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Xóa ngôn ngữ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

