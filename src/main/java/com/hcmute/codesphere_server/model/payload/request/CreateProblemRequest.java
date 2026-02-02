package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProblemRequest {
    
    @NotBlank(message = "Code không được để trống")
    @Size(max = 40, message = "Code không được vượt quá 40 ký tự")
    private String code;
    
    @NotBlank(message = "Title không được để trống")
    @Size(max = 200, message = "Title không được vượt quá 200 ký tự")
    private String title;
    
    @NotBlank(message = "Content không được để trống")
    private String content;
    
    @NotBlank(message = "Level không được để trống")
    @Size(max = 10, message = "Level không được vượt quá 10 ký tự")
    private String level; // EASY/MEDIUM/HARD
    
    private Integer timeLimitMs = 2000;

    private Integer memoryLimitMb = 256; // Default: 256MB

    @NotNull(message = "Category IDs không được để trống")
    private List<Long> categoryIds; // Danh sách category IDs
    
    @NotNull(message = "Language IDs không được để trống")
    private List<Long> languageIds; // Danh sách language IDs
    
    private Boolean isPublic = true; // true = public (hiện trong problem list), false = contest-only (ẩn khỏi problem list)
}

