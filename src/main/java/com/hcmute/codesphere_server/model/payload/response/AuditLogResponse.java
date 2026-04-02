package com.hcmute.codesphere_server.model.payload.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditLogResponse {
    private Long id;

    private Long actorId;
    private String actorUsername;
    private String actorRole;

    private String action;
    private String objectType;
    private Long objectId;
    private String objectLabel;

    private String beforeState;
    private String afterState;
    private String changeSummary;

    private String ipAddress;
    private String userAgent;

    private Instant createdAt;
}
