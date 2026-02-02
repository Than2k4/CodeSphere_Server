package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.*;
import com.hcmute.codesphere_server.model.payload.response.AuthResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.service.common.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<DataResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register/init")
    public ResponseEntity<DataResponse<String>> registerInit(
            @Valid @RequestBody RegisterInitRequest request) {
        try {
            authService.registerInit(request);
            return ResponseEntity.ok(DataResponse.success("Đã gửi OTP đến email. Vui lòng kiểm tra hộp thư."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register/verify")
    public ResponseEntity<DataResponse<AuthResponse>> registerVerify(
            @Valid @RequestBody RegisterVerifyRequest request) {
        try {
            AuthResponse response = authService.registerVerify(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<DataResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    // Endpoint cho Google OAuth2 - Spring Security tự xử lý
    // Frontend redirect đến: /oauth2/authorization/google
    @GetMapping("/google")
    public ResponseEntity<DataResponse<String>> googleAuth() {
        return ResponseEntity.ok(DataResponse.success(
            "Redirect to: /oauth2/authorization/google"
        ));
    }

    @PostMapping("/forgot-password/init")
    public ResponseEntity<DataResponse<String>> forgotPasswordInit(
            @Valid @RequestBody ForgotPasswordInitRequest request) {
        try {
            authService.forgotPasswordInit(request);
            return ResponseEntity.ok(DataResponse.success("Đã gửi OTP đặt lại mật khẩu. Vui lòng kiểm tra email."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<DataResponse<String>> forgotPasswordVerify(
            @Valid @RequestBody ForgotPasswordVerifyRequest request) {
        try {
            authService.forgotPasswordVerify(request);
            return ResponseEntity.ok(DataResponse.success("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<DataResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
            }
            Long userId;
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.hcmute.codesphere_server.security.authentication.UserPrinciple userPrinciple) {
                    userId = Long.parseLong(userPrinciple.getUserId());
                } else {
                    userId = Long.parseLong(authentication.getName());
                }
            } catch (Exception ex) {
                return ResponseEntity.status(401)
                        .body(DataResponse.error("Unauthorized - Không lấy được thông tin người dùng"));
            }
            authService.changePassword(userId, request);
            return ResponseEntity.ok(DataResponse.success("Đổi mật khẩu thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}
