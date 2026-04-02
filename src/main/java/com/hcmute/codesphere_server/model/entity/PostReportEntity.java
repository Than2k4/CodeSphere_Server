package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "post_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_report", columnNames = {"post_id", "reporter_id"})
}, indexes = {
        @Index(name = "idx_post_report_post", columnList = "post_id"),
        @Index(name = "idx_post_report_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @Column(nullable = false, length = 50)
    private String reasonCode;

    @Column(length = 500)
    private String reasonDetail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
