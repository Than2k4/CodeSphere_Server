package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "contest_submissions", indexes = {
    @Index(name = "idx_contest_submission_contest", columnList = "contest_id"),
    @Index(name = "idx_contest_submission_submission", columnList = "submission_id"),
    @Index(name = "idx_contest_submission_created", columnList = "submitted_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private ContestEntity contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private SubmissionEntity submission;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0; // Score for this submission in contest

    @Column(nullable = false, name = "submitted_at")
    @Builder.Default
    private Instant submittedAt = Instant.now();
}

