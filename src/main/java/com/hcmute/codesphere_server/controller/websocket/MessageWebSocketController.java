package com.hcmute.codesphere_server.controller.websocket;

import com.hcmute.codesphere_server.model.payload.response.MessageResponse;
import com.hcmute.codesphere_server.service.common.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    /**
     * Gửi tin nhắn đến conversation
     * Client gửi đến: /app/chat.send
     * Server gửi đến: /topic/conversation/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload) {
        try {
            Long conversationId = Long.parseLong(payload.get("conversationId").toString());
            Long userId = Long.parseLong(payload.get("userId").toString());
            
            // Tạo SendMessageRequest từ payload
            // Đảm bảo messageType luôn có giá trị, mặc định là TEXT nếu không có
            String messageType = payload.get("messageType") != null 
                ? (String) payload.get("messageType") 
                : "TEXT";
            
            com.hcmute.codesphere_server.model.payload.request.SendMessageRequest request = 
                com.hcmute.codesphere_server.model.payload.request.SendMessageRequest.builder()
                    .content((String) payload.get("content"))
                    .messageType(messageType)
                    .imageUrl((String) payload.get("imageUrl"))
                    .fileUrl((String) payload.get("fileUrl"))
                    .fileName((String) payload.get("fileName"))
                    .fileType((String) payload.get("fileType"))
                    .build();
            
            // Lưu message vào database
            MessageResponse message = messageService.sendMessage(conversationId, request, userId);
            
            // Gửi đến tất cả participants trong conversation
            // Sử dụng SimpMessagingTemplate để gửi với destination động
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, message);
            
            log.info("Message sent to conversation {} via WebSocket", conversationId);
        } catch (Exception e) {
            log.error("Error sending message via WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Join conversation
     * Client gửi đến: /app/chat.addUser
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload Map<String, Object> payload) {
        try {
            Long conversationId = Long.parseLong(payload.get("conversationId").toString());
            Long userId = Long.parseLong(payload.get("userId").toString());
            
            log.info("User {} joined conversation {}", userId, conversationId);
            // Có thể gửi notification đến các user khác trong conversation
        } catch (Exception e) {
            log.error("Error adding user to conversation: {}", e.getMessage());
        }
    }
}

