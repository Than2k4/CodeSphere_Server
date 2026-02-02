package com.hcmute.codesphere_server.model.payload.request;

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
public class UpdatePostRequest {
    
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    private String content;
    
    private String imageUrl; // URL hình ảnh đính kèm (deprecated)
    
    private List<String> images; // List of image URLs
    
    private String fileUrl; // URL file đính kèm
    
    private String fileName; // Tên file gốc
    
    private String fileType; // Loại file (pdf, doc, etc.)
    
    private List<String> tagNames; // Tên các tag (có thể tạo tag mới)
}

