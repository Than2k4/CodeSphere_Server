package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTestCaseRequest {
    
    @NotNull(message = "Problem ID không được để trống")
    private Long problemId;
    
    @NotBlank(message = "Input không được để trống")
    private String input;
    
    @NotBlank(message = "Expected Output không được để trống")
    private String expectedOutput;
    
    private Boolean isSample = false;
    
    private Boolean isHidden = false;
    
    private Integer weight = 1;
}

