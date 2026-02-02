package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminLanguageService;
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

    @PostMapping
    public ResponseEntity<DataResponse<LanguageResponse>> createLanguage(
            @Valid @RequestBody CreateLanguageRequest request,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            LanguageResponse response = adminLanguageService.createLanguage(request);
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
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            LanguageResponse response = adminLanguageService.updateLanguage(id, request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteLanguage(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            adminLanguageService.deleteLanguage(id);
            return ResponseEntity.ok(DataResponse.success("Xóa ngôn ngữ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

