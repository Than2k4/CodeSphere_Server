package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeResponse {
    private Boolean success;
    private String message;
    private List<TestResult> testResults;
    private Integer totalPassed;
    private Integer totalTests;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private Long testCaseId;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Boolean isPassed;
        private String runtime;
        private String memory;
        private String errorMessage;
    }
}

