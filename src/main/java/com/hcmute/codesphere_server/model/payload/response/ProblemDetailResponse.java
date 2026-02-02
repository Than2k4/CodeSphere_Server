package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailResponse {
    private Long id;
    private String code;
    private String title;
    private String content; // Đề bài đầy đủ
    private String level; // EASY/MEDIUM/HARD
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Long authorId;
    private String authorName;
    private Boolean isPublic; // true = public (hiện trong problem list), false = contest-only (ẩn khỏi problem list)
    private List<CategoryResponse> categories;
    private List<LanguageResponse> languages;
}

