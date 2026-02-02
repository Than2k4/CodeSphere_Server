package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTagRequest {
    
    @NotBlank(message = "Tên tag không được để trống")
    @Size(max = 80, message = "Tên tag không được vượt quá 80 ký tự")
    private String name;
    
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 120, message = "Slug không được vượt quá 120 ký tự")
    private String slug;
}

