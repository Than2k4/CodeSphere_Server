package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateCategoryRequest;
import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @PostMapping
    public ResponseEntity<DataResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
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
            CategoryResponse response = adminCategoryService.createCategory(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized"));
        }

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden"));
        }

        try {
            adminCategoryService.deleteCategory(id);
            return ResponseEntity.ok(DataResponse.success("Category deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

