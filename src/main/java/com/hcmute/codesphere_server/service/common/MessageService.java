package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import com.hcmute.codesphere_server.model.entity.MessageEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.enums.MessageType;
import com.hcmute.codesphere_server.model.payload.request.SendMessageRequest;
import com.hcmute.codesphere_server.model.payload.response.MessageResponse;
import com.hcmute.codesphere_server.repository.common.ConversationParticipantRepository;
import com.hcmute.codesphere_server.repository.common.ConversationRepository;
import com.hcmute.codesphere_server.repository.common.MessageRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        UserEntity sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(request.getMessageType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("MessageType không hợp lệ. Phải là TEXT, IMAGE hoặc FILE");
        }

        // Validate
        if (messageType == MessageType.TEXT) {
            if (request.getContent() == null || request.getContent().isEmpty()) {
                throw new RuntimeException("TEXT message cần có content");
            }
        } else if (messageType == MessageType.IMAGE) {
            if (request.getImageUrl() == null || request.getImageUrl().isEmpty()) {
                throw new RuntimeException("IMAGE message cần có imageUrl");
            }
        } else if (messageType == MessageType.FILE) {
            if (request.getFileUrl() == null || request.getFileUrl().isEmpty()) {
                throw new RuntimeException("FILE message cần có fileUrl");
            }
        }

        MessageEntity message = MessageEntity.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        // Cập nhật updatedAt của conversation
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Gửi notification cho tất cả participants (trừ sender)
        try {
            var participants = conversationParticipantRepository.findByConversationId(conversationId);
            String conversationName = conversation.getName();
            ConversationEntity.ConversationType conversationType = conversation.getType();
            
            for (var participant : participants) {
                if (participant.getUser() != null && !participant.getUser().getId().equals(userId)) {
                    notificationService.notifyMessage(
                            participant.getUser().getId(),
                            userId,
                            sender.getUsername(),
                            conversationId,
                            conversationName,
                            conversationType
                    );
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, Long userId, Pageable pageable) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        Page<MessageEntity> messages = messageRepository.findByConversationId(conversationId, pageable);
        return messages.map(this::mapToMessageResponse);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        MessageEntity message = messageRepository.findByIdAndNotDeleted(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn"));

        // Kiểm tra quyền sở hữu
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa tin nhắn này");
        }

        message.setIsDeleted(true);
        message.setDeletedAt(Instant.now());
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);
        
        // Cập nhật updatedAt của conversation
        if (message.getConversation() != null) {
            message.getConversation().setUpdatedAt(Instant.now());
            conversationRepository.save(message.getConversation());
        }
    }

    @Transactional
    public MessageResponse createSystemMessage(Long conversationId, String content) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation"));
        
        // System message không có sender (hoặc có thể dùng system user)
        // Tạm thời dùng createdBy của conversation làm sender
        UserEntity systemSender = conversation.getCreatedBy();
        
        MessageEntity message = MessageEntity.builder()
                .conversation(conversation)
                .sender(systemSender) // System message vẫn cần sender để tránh null
                .content(content)
                .messageType(MessageType.SYSTEM)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        message = messageRepository.save(message);
        
        // Cập nhật updatedAt của conversation
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        
        MessageResponse messageResponse = mapToMessageResponse(message);
        
        // Gửi system message qua WebSocket để frontend nhận real-time
        try {
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, messageResponse);
        } catch (Exception e) {
            // Log error nhưng không throw
            System.err.println("Error sending system message via WebSocket: " + e.getMessage());
        }
        
        return messageResponse;
    }

    private MessageResponse mapToMessageResponse(MessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation() != null ? entity.getConversation().getId() : null)
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getUsername() : null)
                .senderAvatar(entity.getSender() != null ? entity.getSender().getAvatar() : null)
                .content(entity.getContent())
                .messageType(entity.getMessageType() != null ? entity.getMessageType().name() : null)
                .imageUrl(entity.getImageUrl())
                .fileUrl(entity.getFileUrl())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .isDeleted(entity.getIsDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

