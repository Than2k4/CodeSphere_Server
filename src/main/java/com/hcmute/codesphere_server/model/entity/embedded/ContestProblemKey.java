package com.hcmute.codesphere_server.model.entity.embedded;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemKey implements Serializable {

    private Long contestId;
    private Long problemId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContestProblemKey that = (ContestProblemKey) o;
        return Objects.equals(contestId, that.contestId) &&
                Objects.equals(problemId, that.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contestId, problemId);
    }
}

