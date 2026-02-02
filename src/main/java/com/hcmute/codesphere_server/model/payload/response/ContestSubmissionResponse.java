package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestSubmissionResponse {
    private Long id;
    private Long submissionId;
    private Long problemId;
    private String problemTitle;
    private String problemOrder; // A, B, C
    private Integer score;
    private Boolean isAccepted;
    private String language;
    private Instant submittedAt;
    private String statusMsg;
    private Integer runtime;
    private Integer memory;
    private String codeContent; // Code đã submit
}

