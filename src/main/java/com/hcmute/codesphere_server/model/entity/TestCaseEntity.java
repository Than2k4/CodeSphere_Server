package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "test_cases", indexes = {
	@Index(name = "idx_testcase_problem", columnList = "problem_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", nullable = false)
	private ProblemEntity problem;

	@Lob
	@Column(nullable = false)
	private String input;

	@Lob
	@Column(nullable = false)
	private String expectedOutput;

	@Column(nullable = false)
	private Boolean isSample = false;

	@Column(nullable = false)
	private Boolean isHidden = false;

	@Column(nullable = false)
	private Integer weight = 1;

	@Column(nullable = false)
	private Boolean isDeleted = false;
}


