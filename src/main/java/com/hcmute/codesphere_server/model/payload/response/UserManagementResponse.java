package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status; // ACTIVE, INACTIVE, BLOCKED
    private Boolean isBlocked;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Long totalSubmissions;
    private Long totalSolved;
}

