package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private Long id;
    private String type; // FRIEND_REQUEST, FRIEND_ACCEPTED, POST_LIKE, POST_COMMENT, COMMENT_REPLY, MESSAGE
    private String title;
    private String content;
    private Long relatedUserId;
    private String relatedUserName;
    private String relatedUserAvatar;
    private Long relatedPostId;
    private Long relatedCommentId;
    private Long relatedConversationId;
    private Long relatedContestId;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}

