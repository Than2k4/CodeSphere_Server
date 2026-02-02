package com.hcmute.codesphere_server.model.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionTestcaseKey implements Serializable {

    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "test_case_id")
    private Long testCaseId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubmissionTestcaseKey that = (SubmissionTestcaseKey) o;
        return java.util.Objects.equals(submissionId, that.submissionId) &&
                java.util.Objects.equals(testCaseId, that.testCaseId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(submissionId, testCaseId);
    }
}

