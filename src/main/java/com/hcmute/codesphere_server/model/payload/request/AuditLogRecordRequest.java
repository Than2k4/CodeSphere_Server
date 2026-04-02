package com.hcmute.codesphere_server.model.payload.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogRecordRequest {
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
}
