package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.TestCaseResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminTestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/testcases")
@RequiredArgsConstructor
public class AdminTestCaseController {

    private final AdminTestCaseService adminTestCaseService;

    @PostMapping
    public ResponseEntity<DataResponse<TestCaseResponse>> createTestCase(
            @Valid @RequestBody CreateTestCaseRequest request,
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
            TestCaseResponse response = adminTestCaseService.createTestCase(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<TestCaseResponse>> updateTestCase(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestCaseRequest request,
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
            TestCaseResponse response = adminTestCaseService.updateTestCase(id, request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteTestCase(
            @PathVariable Long id,
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
            adminTestCaseService.deleteTestCase(id);
            return ResponseEntity.ok(DataResponse.success("Testcase đã được xóa thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

