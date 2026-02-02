package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "submissions", indexes = {
	@Index(name = "idx_submission_user", columnList = "user_id"),
	@Index(name = "idx_submission_problem", columnList = "problem_id"),
	@Index(name = "idx_submission_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEntity {

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
	@JoinColumn(name = "language_id", nullable = false)
	private LanguageEntity language;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String codeContent;

	@Column(nullable = false)
	private Boolean isAccepted = false;

	@Column(nullable = false)
	private Integer score = 0;

	@Column(nullable = false)
	private Integer statusCode;

	@Column(nullable = false, length = 60)
	private String statusRuntime;

	@Column(nullable = false)
	private Integer memoryKb;

	@Column(length = 60)
	private String displayRuntime;

	@Column(nullable = false)
	private Integer totalCorrect;

	@Column(nullable = false)
	private Integer totalTestcases;

	@Column(nullable = false, length = 60)
	private String statusMemory;

	@Column(nullable = false, length = 120)
	private String statusMsg;

	@Column(nullable = false, length = 60)
	private String state;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String compileError;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String fullCompileError;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }
}


