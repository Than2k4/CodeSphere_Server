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
public class ConversationResponse {
    private Long id;
    private String type; // DIRECT, GROUP
    private String name;
    private String avatar;
    private Long createdById;
    private String createdByName;
    private List<ConversationParticipantResponse> participants;
    private MessageResponse lastMessage; // Tin nhắn cuối cùng
    private Long unreadCount; // Số tin nhắn chưa đọc
    private Instant createdAt;
    private Instant updatedAt;
}

