package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RunCodeRequest {
    
    @NotNull(message = "Problem ID không được để trống")
    private Long problemId;
    
    @NotNull(message = "Language ID không được để trống")
    private Long languageId;
    
    @NotBlank(message = "Code content không được để trống")
    private String codeContent;
    
    // Optional: Array of custom test cases (sẽ chạy cùng với sample testcases)
    private List<CustomTestCase> customTestCases;
    
    @Data
    public static class CustomTestCase {
        private String input;
        private String expectedOutput; // Optional
    }
}

