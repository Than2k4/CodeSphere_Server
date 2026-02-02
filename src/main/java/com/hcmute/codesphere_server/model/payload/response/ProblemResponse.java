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
public class ProblemResponse {
    private Long id;
    private String code;
    private String title;
    private String level; // EASY/MEDIUM/HARD
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Long authorId;
    private String authorName;
    private List<CategoryResponse> categories;
    private List<LanguageResponse> languages;
    
    // Thông tin trạng thái của problem (cho admin)
    private Boolean isPublic; // true = public (hiện trong problem list), false = contest-only (ẩn khỏi problem list)
    
    // Thông tin trạng thái của user (chỉ có khi user đã đăng nhập)
    private Boolean isBookmarked; // Đã đánh dấu sao chưa (độc lập, có thể kết hợp với bất kỳ status nào)
    private ProblemStatus status;  // Trạng thái làm bài: NOT_ATTEMPTED, ATTEMPTED_NOT_COMPLETED, COMPLETED
    
    /**
     * Enum trạng thái làm bài của user
     */
    public enum ProblemStatus {
        NOT_ATTEMPTED,              // Chưa làm
        ATTEMPTED_NOT_COMPLETED,    // Chưa hoàn thành (đã thử nhưng chưa đúng)
        COMPLETED                   // Đã hoàn thành (đã giải đúng)
    }
}

