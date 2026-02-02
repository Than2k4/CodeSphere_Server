package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubmissionRequest {
    
    @NotNull(message = "Problem ID không được để trống")
    private Long problemId;
    
    @NotNull(message = "Language ID không được để trống")
    private Long languageId;
    
    @NotBlank(message = "Code content không được để trống")
    private String codeContent;
}

