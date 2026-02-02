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
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}

