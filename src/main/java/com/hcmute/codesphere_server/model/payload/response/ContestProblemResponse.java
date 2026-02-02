package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestProblemResponse {
    private Long problemId;
    private String order; // A, B, C, D, etc.
    private Integer points;
    private ProblemResponse problem; // Full problem details
    private Boolean isSolved; // Whether current user has solved it
    private Integer bestScore; // Best score for this problem in contest
}

