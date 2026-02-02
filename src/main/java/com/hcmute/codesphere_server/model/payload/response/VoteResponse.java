package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResponse {
    private Long id; // postId hoặc commentId
    private Integer vote; // 1, -1, hoặc 0 (removed)
    private Long totalVotes;
    private Long upvotes;
    private Long downvotes;
    private String message;
}

