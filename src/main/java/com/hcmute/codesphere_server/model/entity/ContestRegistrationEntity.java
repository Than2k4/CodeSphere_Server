package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.ContestRegistrationKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "contest_registrations", indexes = {
    @Index(name = "idx_contest_registration_contest", columnList = "contest_id"),
    @Index(name = "idx_contest_registration_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestRegistrationEntity {

    @EmbeddedId
    private ContestRegistrationKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contestId")
    @JoinColumn(name = "contest_id", nullable = false)
    private ContestEntity contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, name = "registered_at")
    @Builder.Default
    private Instant registeredAt = Instant.now();

    @Column(name = "started_at", nullable = true)
    private Instant startedAt; // Thời gian user bắt đầu làm (chỉ cho PRACTICE contest)

    @Column(name = "ended_at", nullable = true)
    private Instant endedAt; // Thời gian user kết thúc (chỉ cho PRACTICE contest)

    @Column(name = "attempt_count", nullable = true)
    @Builder.Default
    private Integer attemptCount = 0; // Số lần đã làm (chỉ cho PRACTICE contest)

    // Best attempt info (chỉ cho PRACTICE contest)
    @Column(name = "best_total_score", nullable = true)
    private Integer bestTotalScore; // Điểm tổng tốt nhất

    @Column(name = "best_completion_time_seconds", nullable = true)
    private Long bestCompletionTimeSeconds; // Thời gian hoàn thành tốt nhất (giây)

    @Column(name = "best_started_at", nullable = true)
    private Instant bestStartedAt; // Thời gian bắt đầu của attempt tốt nhất

    @Column(name = "best_ended_at", nullable = true)
    private Instant bestEndedAt; // Thời gian kết thúc của attempt tốt nhất
}

