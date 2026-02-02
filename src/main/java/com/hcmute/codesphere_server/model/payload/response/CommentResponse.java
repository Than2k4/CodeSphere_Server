package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private Long id;
    private String content;
    private Boolean isAccepted;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private Long parentCommentId;
    private Long totalVotes;
    private Long upvotes;
    private Long downvotes;
    private Integer userVote; // null, 1 (upvote), -1 (downvote)
    private Long replyCount;
    private List<CommentResponse> replies;
    private Instant createdAt;
    private Instant updatedAt;
}

