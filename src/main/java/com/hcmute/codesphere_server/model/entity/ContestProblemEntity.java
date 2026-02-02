package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.ContestProblemKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contest_problems", indexes = {
    @Index(name = "idx_contest_problem_contest", columnList = "contest_id"),
    @Index(name = "idx_contest_problem_problem", columnList = "problem_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemEntity {

    @EmbeddedId
    private ContestProblemKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contestId")
    @JoinColumn(name = "contest_id", nullable = false)
    private ContestEntity contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("problemId")
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(name = "problem_order", nullable = false, length = 10)
    private String problemOrder; // A, B, C, D, etc.

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 100;
}

