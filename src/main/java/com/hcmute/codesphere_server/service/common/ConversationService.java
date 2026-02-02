package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import com.hcmute.codesphere_server.model.entity.ConversationParticipantEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.enums.ParticipantRole;
import com.hcmute.codesphere_server.model.payload.request.AddMemberRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateConversationRequest;
import com.hcmute.codesphere_server.model.payload.request.TransferAdminRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateConversationRequest;
import com.hcmute.codesphere_server.model.payload.response.ConversationParticipantResponse;
import com.hcmute.codesphere_server.model.payload.response.ConversationResponse;
import com.hcmute.codesphere_server.model.payload.response.MessageResponse;
import com.hcmute.codesphere_server.repository.common.ConversationParticipantRepository;
import com.hcmute.codesphere_server.repository.common.ConversationRepository;
import com.hcmute.codesphere_server.repository.common.MessageRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Long userId) {
        UserEntity creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        ConversationEntity.ConversationType type;
        try {
            type = ConversationEntity.ConversationType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type không hợp lệ. Phải là DIRECT hoặc GROUP");
        }

        // Validate DIRECT conversation
        if (type == ConversationEntity.ConversationType.DIRECT) {
            if (request.getParticipantIds().size() != 1) {
                throw new RuntimeException("DIRECT conversation chỉ có thể có 1 participant (người nhận)");
            }
            
            // Kiểm tra đã có direct conversation chưa
            Long otherUserId = request.getParticipantIds().get(0);
            var existing = conversationRepository.findDirectConversation(userId, otherUserId);
            if (existing.isPresent()) {
                return mapToConversationResponse(existing.get(), userId);
            }
        }

        // Validate GROUP conversation
        if (type == ConversationEntity.ConversationType.GROUP) {
            if (request.getName() == null || request.getName().isEmpty()) {
                throw new RuntimeException("GROUP conversation cần có tên");
            }
            if (request.getParticipantIds().isEmpty()) {
                throw new RuntimeException("GROUP conversation cần có ít nhất 1 participant");
            }
        }

        ConversationEntity conversation = ConversationEntity.builder()
                .type(type)
                .name(request.getName())
                .avatar(request.getAvatar())
                .createdBy(creator)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        conversation = conversationRepository.save(conversation);

        // Lấy danh sách participants (bao gồm creator)
        List<UserEntity> participantsList = new java.util.ArrayList<>();
        participantsList.add(creator);
        
        // Thêm các participants khác
        Set<UserEntity> otherParticipants = request.getParticipantIds().stream()
                .map(participantId -> userRepository.findById(participantId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + participantId)))
                .collect(Collectors.toSet());
        participantsList.addAll(otherParticipants);

        // Tạo ConversationParticipantEntity cho từng participant
        for (UserEntity participant : participantsList) {
            ParticipantRole role = 
                    participant.getId().equals(userId) && type == ConversationEntity.ConversationType.GROUP
                            ? ParticipantRole.ADMIN
                            : ParticipantRole.MEMBER;
            
            // Dùng constructor thay vì builder để đảm bảo @PrePersist được gọi
            ConversationParticipantEntity cp = new ConversationParticipantEntity();
            cp.setConversation(conversation);
            cp.setUser(participant);
            cp.setRole(role);
            cp.setJoinedAt(Instant.now());
            conversationParticipantRepository.save(cp);
        }

        return mapToConversationResponse(conversation, userId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        List<ConversationEntity> conversations = conversationRepository.findByParticipantId(userId);
        return conversations.stream()
                .map(conv -> mapToConversationResponse(conv, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Long conversationId, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));
        
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public ConversationResponse updateConversation(Long conversationId, UpdateConversationRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể update
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể chỉnh sửa");
        }

        // Kiểm tra quyền ADMIN (người tạo nhóm ban đầu được set role = ADMIN)
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (participant.isEmpty() || 
            participant.get().getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền chỉnh sửa conversation");
        }

        if (request.getName() != null) {
            conversation.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            conversation.setAvatar(request.getAvatar());
        }

        conversation.setUpdatedAt(Instant.now());
        conversation = conversationRepository.save(conversation);
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public ConversationResponse addMember(Long conversationId, AddMemberRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể thêm member
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể thêm thành viên");
        }

        // Kiểm tra quyền ADMIN (người tạo nhóm ban đầu được set role = ADMIN)
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (participant.isEmpty() || 
            participant.get().getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền thêm thành viên");
        }

        for (Long newUserId : request.getUserIds()) {
            // Kiểm tra đã là participant chưa
            var existingParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, newUserId);
            if (existingParticipant.isPresent()) {
                continue; // Đã là member rồi
            }

            UserEntity newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + newUserId));

            // Dùng constructor thay vì builder để đảm bảo @PrePersist được gọi
            ConversationParticipantEntity cp = new ConversationParticipantEntity();
            cp.setConversation(conversation);
            cp.setUser(newUser);
            cp.setRole(ParticipantRole.MEMBER);
            cp.setJoinedAt(Instant.now());
            conversationParticipantRepository.save(cp);
            
            // Tạo system message: "X đã thêm Y vào nhóm"
            UserEntity adder = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            String systemMessage = adder.getUsername() + " đã thêm " + newUser.getUsername() + " vào nhóm";
            try {
                messageService.createSystemMessage(conversationId, systemMessage);
            } catch (Exception e) {
                // Log error nhưng không throw
                System.err.println("Error creating system message: " + e.getMessage());
            }
        }

        conversation.setUpdatedAt(Instant.now());
        conversation = conversationRepository.save(conversation);
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public void removeMember(Long conversationId, Long memberId, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể xóa member
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể xóa thành viên");
        }

        // Kiểm tra quyền: chỉ ADMIN mới có quyền xóa thành viên khác, hoặc tự xóa chính mình
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        boolean isAdmin = participant.isPresent() && 
                         participant.get().getRole() == ParticipantRole.ADMIN;
        
        if (!isAdmin && !memberId.equals(userId)) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền xóa thành viên khác");
        }

        // Kiểm tra nếu đang xóa ADMIN
        var memberParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, memberId);
        boolean isRemovingAdmin = memberParticipant.isPresent() && 
                                  memberParticipant.get().getRole() == ParticipantRole.ADMIN;
        
        // Nếu đang xóa ADMIN (tự xóa chính mình), không cho phép trừ khi đã transfer admin
        // Logic transfer admin sẽ được xử lý ở endpoint leaveGroup riêng
        if (isRemovingAdmin && memberId.equals(userId)) {
            throw new RuntimeException("Bạn không thể rời nhóm khi đang là ADMIN. Vui lòng bổ nhiệm thành viên khác làm ADMIN trước.");
        }

        if (memberParticipant.isPresent()) {
            UserEntity removedUser = memberParticipant.get().getUser();
            boolean isSelfRemove = memberId.equals(userId);
            
            conversationParticipantRepository.delete(memberParticipant.get());
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
            
            // Tạo system message về việc rời/xóa nhóm
            String systemMessage;
            if (isSelfRemove) {
                // "X đã rời nhóm"
                systemMessage = removedUser.getUsername() + " đã rời nhóm";
            } else {
                // "X đã xóa Y khỏi nhóm"
                UserEntity remover = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
                systemMessage = remover.getUsername() + " đã xóa " + removedUser.getUsername() + " khỏi nhóm";
            }
            
            try {
                messageService.createSystemMessage(conversationId, systemMessage);
            } catch (Exception e) {
                // Log error nhưng không throw
                System.err.println("Error creating system message: " + e.getMessage());
            }
        }
    }

    @Transactional
    public ConversationResponse transferAdmin(Long conversationId, TransferAdminRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể transfer admin
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể bổ nhiệm ADMIN");
        }

        // Kiểm tra quyền: chỉ ADMIN hiện tại mới có thể transfer admin
        var currentAdminParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (currentAdminParticipant.isEmpty() || 
            currentAdminParticipant.get().getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN hiện tại mới có quyền bổ nhiệm ADMIN mới");
        }

        // Kiểm tra newAdminId có phải là member không
        var newAdminParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, request.getNewAdminId());
        if (newAdminParticipant.isEmpty()) {
            throw new RuntimeException("Người dùng không phải là thành viên của nhóm");
        }

        if (newAdminParticipant.get().getRole() == ParticipantRole.ADMIN) {
            throw new RuntimeException("Người dùng này đã là ADMIN");
        }

        // Transfer admin: set role ADMIN cho newAdmin, set role MEMBER cho currentAdmin
        newAdminParticipant.get().setRole(ParticipantRole.ADMIN);
        currentAdminParticipant.get().setRole(ParticipantRole.MEMBER);
        conversationParticipantRepository.save(newAdminParticipant.get());
        conversationParticipantRepository.save(currentAdminParticipant.get());

        conversation.setUpdatedAt(Instant.now());
        conversation = conversationRepository.save(conversation);

        // Tạo system message
        UserEntity currentAdmin = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        UserEntity newAdmin = newAdminParticipant.get().getUser();
        String systemMessage = currentAdmin.getUsername() + " đã bổ nhiệm " + newAdmin.getUsername() + " làm trưởng nhóm";
        
        try {
            messageService.createSystemMessage(conversationId, systemMessage);
        } catch (Exception e) {
            System.err.println("Error creating transfer admin system message: " + e.getMessage());
        }

        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public void leaveGroupWithTransfer(Long conversationId, Long userId, Long newAdminId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể rời nhóm
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể rời nhóm");
        }

        // Kiểm tra user có phải là participant không
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (participant.isEmpty()) {
            throw new RuntimeException("Bạn không phải là thành viên của nhóm");
        }

        boolean isAdmin = participant.get().getRole() == ParticipantRole.ADMIN;

        // Nếu là ADMIN, phải transfer admin trước khi rời nhóm
        if (isAdmin) {
            if (newAdminId == null) {
                throw new RuntimeException("Bạn không thể rời nhóm khi đang là ADMIN. Vui lòng bổ nhiệm thành viên khác làm ADMIN trước.");
            }

            // Kiểm tra newAdminId có phải là member không
            var newAdminParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, newAdminId);
            if (newAdminParticipant.isEmpty()) {
                throw new RuntimeException("Người dùng không phải là thành viên của nhóm");
            }

            if (newAdminId.equals(userId)) {
                throw new RuntimeException("Không thể bổ nhiệm chính mình làm ADMIN");
            }

            if (newAdminParticipant.get().getRole() == ParticipantRole.ADMIN) {
                throw new RuntimeException("Người dùng này đã là ADMIN");
            }

            // Transfer admin
            newAdminParticipant.get().setRole(ParticipantRole.ADMIN);
            conversationParticipantRepository.save(newAdminParticipant.get());

            // Tạo system message về transfer admin
            UserEntity currentAdmin = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            UserEntity newAdmin = newAdminParticipant.get().getUser();
            String transferMessage = currentAdmin.getUsername() + " đã bổ nhiệm " + newAdmin.getUsername() + " làm trưởng nhóm";
            
            try {
                messageService.createSystemMessage(conversationId, transferMessage);
            } catch (Exception e) {
                System.err.println("Error creating transfer admin system message: " + e.getMessage());
            }
        }

        // Xóa user khỏi nhóm
        UserEntity leavingUser = participant.get().getUser();
        conversationParticipantRepository.delete(participant.get());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Tạo system message về việc rời nhóm
        String leaveMessage = leavingUser.getUsername() + " đã rời nhóm";
        try {
            messageService.createSystemMessage(conversationId, leaveMessage);
        } catch (Exception e) {
            System.err.println("Error creating leave group system message: " + e.getMessage());
        }
    }

    private ConversationResponse mapToConversationResponse(ConversationEntity entity, Long userId) {
        // Lấy last message
        MessageResponse lastMessage = null;
        var lastMessages = messageRepository.findAllByConversationId(entity.getId());
        if (!lastMessages.isEmpty()) {
            lastMessage = mapToMessageResponse(lastMessages.get(0));
        }

        // Lấy participants
        List<ConversationParticipantResponse> participants = conversationParticipantRepository
                .findByConversationId(entity.getId())
                .stream()
                .map(this::mapToParticipantResponse)
                .collect(Collectors.toList());

        return ConversationResponse.builder()
                .id(entity.getId())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .name(entity.getName())
                .avatar(entity.getAvatar())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .participants(participants)
                .lastMessage(lastMessage)
                .unreadCount(0L) // TODO: Implement unread count logic
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ConversationParticipantResponse mapToParticipantResponse(ConversationParticipantEntity entity) {
        return ConversationParticipantResponse.builder()
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .avatar(entity.getUser() != null ? entity.getUser().getAvatar() : null)
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .joinedAt(entity.getJoinedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(com.hcmute.codesphere_server.model.entity.MessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation() != null ? entity.getConversation().getId() : null)
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getUsername() : null)
                .senderAvatar(entity.getSender() != null ? entity.getSender().getAvatar() : null)
                .content(entity.getContent())
                .messageType(entity.getMessageType() != null ? entity.getMessageType().name() : null)
                .imageUrl(entity.getImageUrl())
                .isDeleted(entity.getIsDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

