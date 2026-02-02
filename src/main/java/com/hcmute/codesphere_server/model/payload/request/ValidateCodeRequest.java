package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateCodeRequest {
    
    @NotBlank(message = "Code content không được để trống")
    private String codeContent;
    
    @NotBlank(message = "Language code không được để trống")
    private String languageCode;
}

