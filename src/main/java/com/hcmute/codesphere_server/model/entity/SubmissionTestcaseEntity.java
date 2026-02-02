package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.entity.embedded.SubmissionTestcaseKey;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "submission_testcases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionTestcaseEntity {

	@EmbeddedId
	private SubmissionTestcaseKey id = new SubmissionTestcaseKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("submissionId")
	@JoinColumn(name = "submission_id")
	private SubmissionEntity submission;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("testCaseId")
	@JoinColumn(name = "test_case_id")
	private TestCaseEntity testCase;

	@Column(nullable = false, length = 10)
	private String status; // PASSED/FAILED/...

	@Column
	private Integer runtimeMs;

	@Column
	private Integer memoryKb;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String stdout;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String stderr;

	@Column(nullable = false)
	private Boolean isDeleted = false;
}



