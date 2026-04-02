package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSolutionResponse {
    private Long submissionId;
    private Long userId;
    private String username;
    private Integer rank;
    private Integer bestScore;
    private String languageName;
    private String languageCode;
    private String statusRuntime;
    private String statusMemory;
    private Instant submittedAt;
    private String codeContent;
}