package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionSummaryResponse {
    private Long postId;
    private Long totalReactions;
    private List<ReactionSummaryItemResponse> topReactions;
}
