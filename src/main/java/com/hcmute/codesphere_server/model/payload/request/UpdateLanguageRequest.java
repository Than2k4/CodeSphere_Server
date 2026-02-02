package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLanguageRequest {
    
    @Size(max = 20, message = "Code không được vượt quá 20 ký tự")
    private String code;
    
    @Size(max = 50, message = "Tên ngôn ngữ không được vượt quá 50 ký tự")
    private String name;
    
    @Size(max = 30, message = "Version không được vượt quá 30 ký tự")
    private String version;
}

