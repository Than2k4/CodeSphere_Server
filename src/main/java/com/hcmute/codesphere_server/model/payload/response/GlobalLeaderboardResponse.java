package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalLeaderboardResponse {
    
    private Integer rank; // Thứ hạng (1, 2, 3, ...)
    
    private Long userId;
    
    private String username;
    
    private Integer totalSolved; // Tổng số bài đã giải đúng
    
    private Integer solvedEasy; // Số bài EASY đã giải
    
    private Integer solvedMedium; // Số bài MEDIUM đã giải
    
    private Integer solvedHard; // Số bài HARD đã giải
}

