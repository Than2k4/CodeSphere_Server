package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity lưu best submission của mỗi user cho mỗi problem
 * Được cập nhật tự động khi user nộp submission mới có điểm cao hơn
 */
@Entity
@Table(name = "user_problem_best", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}),
       indexes = {
           @Index(name = "idx_user_problem_best_user", columnList = "user_id"),
           @Index(name = "idx_user_problem_best_problem", columnList = "problem_id"),
           @Index(name = "idx_user_problem_best_score", columnList = "best_score")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProblemBestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_submission_id", nullable = false)
    private SubmissionEntity bestSubmission;

    @Column(nullable = false)
    private Integer bestScore = 0;

    @Column(nullable = false)
    private Integer totalSubmissions = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}

