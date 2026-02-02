package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    
    @NotBlank(message = "Tên category không được để trống")
    @Size(max = 120, message = "Tên category không được vượt quá 120 ký tự")
    private String name;
    
    // Slug sẽ được tự động tạo từ name, không cần nhập
}

