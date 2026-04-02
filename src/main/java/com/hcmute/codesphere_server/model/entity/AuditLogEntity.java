package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_created", columnList = "createdAt"),
        @Index(name = "idx_audit_actor", columnList = "actorId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_object", columnList = "objectType,objectId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;

    @Column(length = 100)
    private String actorUsername;

    @Column(length = 50)
    private String actorRole;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String objectType;

    private Long objectId;

    @Column(length = 200)
    private String objectLabel;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String beforeState;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String afterState;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String changeSummary;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
