package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyAccessCodeRequest {
    @NotBlank(message = "Access code is required")
    private String accessCode;
}

