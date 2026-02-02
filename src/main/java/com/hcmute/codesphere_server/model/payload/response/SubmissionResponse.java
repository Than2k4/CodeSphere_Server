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
public class SubmissionResponse {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String problemCode;
    private Long languageId;
    private String languageName;
    private String languageCode;
    private Boolean isAccepted;
    private Integer score;
    private String statusMsg;
    private String statusRuntime;
    private String statusMemory;
    private Integer totalCorrect;
    private Integer totalTestcases;
    private String state;
    private Instant createdAt;
}

