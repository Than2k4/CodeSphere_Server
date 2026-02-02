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
public class SubmissionDetailResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long problemId;
    private String problemTitle;
    private String problemCode;
    private Long languageId;
    private String languageName;
    private String languageCode;
    private String codeContent;
    private Boolean isAccepted;
    private Integer score;
    private Integer statusCode;
    private String statusRuntime;
    private String displayRuntime;
    private Integer memoryKb;
    private String statusMemory;
    private String statusMsg;
    private String state;
    private Integer totalCorrect;
    private Integer totalTestcases;
    private String compileError;
    private String fullCompileError;
    private Instant createdAt;
    private Instant updatedAt;
}

