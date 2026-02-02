package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.response.DashboardStatsResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${base.url}/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DataResponse<DashboardStatsResponse>> getDashboardStats(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            DashboardStatsResponse stats = adminStatisticsService.getDashboardStats();
            return ResponseEntity.ok(DataResponse.success(stats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

