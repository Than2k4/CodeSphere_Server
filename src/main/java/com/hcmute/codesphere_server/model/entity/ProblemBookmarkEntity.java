package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity lưu bookmark (đánh dấu sao) của user cho problem
 */
@Entity
@Table(name = "problem_bookmarks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}),
       indexes = {
           @Index(name = "idx_bookmark_user", columnList = "user_id"),
           @Index(name = "idx_bookmark_problem", columnList = "problem_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemBookmarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PreUpdate
    public void touchUpdatedAt() {
        // Không cần updatedAt cho bookmark, chỉ cần createdAt
    }
}

