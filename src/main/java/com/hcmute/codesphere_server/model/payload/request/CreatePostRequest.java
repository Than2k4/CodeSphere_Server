package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    private String imageUrl; // URL hình ảnh đính kèm (deprecated)
    
    private List<String> images; // List of image URLs
    
    private String fileUrl; // URL file đính kèm
    
    private String fileName; // Tên file gốc
    
    private String fileType; // Loại file (pdf, doc, etc.)
    
    private Boolean isAnonymous = false;
    
    private List<String> tagNames; // Tên các tag (có thể tạo tag mới)
}

