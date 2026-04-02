package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import com.hcmute.codesphere_server.model.entity.NotificationEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.response.NotificationResponse;
import com.hcmute.codesphere_server.repository.common.NotificationRepository;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationEntity createNotification(Long userId, NotificationEntity.NotificationType type,
                                                  String title, String content, Long relatedUserId,
                                                  Long relatedPostId, Long relatedCommentId, Long relatedConversationId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .relatedUserId(relatedUserId)
                .relatedPostId(relatedPostId)
                .relatedCommentId(relatedCommentId)
                .relatedConversationId(relatedConversationId)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);

        // Gửi notification real-time qua WebSocket
        try {
            NotificationResponse response = mapToNotificationResponse(notification);
            String destination = "/user/" + userId.toString() + "/queue/notifications";
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
            System.out.println("Notification sent via WebSocket to user: " + userId + ", destination: " + destination);
        } catch (Exception e) {
            // Log error nhưng vẫn trả về notification đã lưu
            System.err.println("Error sending notification via WebSocket to user: " + userId);
            e.printStackTrace();
        }

        return notification;
    }

    @Transactional
    public NotificationEntity createContestNotification(Long userId, String title, String content, Long relatedContestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .type(NotificationEntity.NotificationType.CONTEST_REMINDER)
                .title(title)
                .content(content)
                .relatedContestId(relatedContestId)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);

        // Gửi notification real-time qua WebSocket
        try {
            NotificationResponse response = mapToNotificationResponse(notification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
            System.out.println("Contest notification sent via WebSocket to user: " + userId);
        } catch (Exception e) {
            System.err.println("Error sending contest notification via WebSocket to user: " + userId);
            e.printStackTrace();
        }

        return notification;
    }

    @Transactional(readOnly = true)
    public boolean hasContestNotification(Long userId, Long contestId, Instant since) {
        return notificationRepository.existsByUserIdAndTypeAndContestIdSince(
                userId,
                NotificationEntity.NotificationType.CONTEST_REMINDER,
                contestId,
                since
        );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, String type, Boolean isRead, Pageable pageable) {
        Page<NotificationEntity> notifications;
        
        if (isRead != null && !isRead) {
            notifications = notificationRepository.findUnreadByUserId(userId, pageable);
        } else if (type != null && !type.isEmpty()) {
            try {
                NotificationEntity.NotificationType notificationType = 
                        NotificationEntity.NotificationType.valueOf(type.toUpperCase());
                notifications = notificationRepository.findByUserIdAndType(userId, notificationType, pageable);
            } catch (IllegalArgumentException e) {
                notifications = notificationRepository.findByUserId(userId, pageable);
            }
        } else {
            notifications = notificationRepository.findByUserId(userId, pageable);
        }

        return notifications.map(this::mapToNotificationResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền đánh dấu notification này");
        }

        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        return mapToNotificationResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Page<NotificationEntity> unreadNotifications = notificationRepository.findUnreadByUserId(
                userId, 
                org.springframework.data.domain.Pageable.unpaged());
        
        Instant now = Instant.now();
        for (NotificationEntity notification : unreadNotifications.getContent()) {
            notification.setIsRead(true);
            notification.setReadAt(now);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa notification này");
        }

        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // Helper methods để tạo notifications từ các services khác
    public void notifyFriendRequest(Long receiverId, Long senderId, String senderName) {
        createNotification(
                receiverId,
                NotificationEntity.NotificationType.FRIEND_REQUEST,
                "Lời mời kết bạn",
                senderName + " đã gửi lời mời kết bạn",
                senderId,
                null, null, null
        );
    }

    public void notifyFriendAccepted(Long userId, Long friendId, String friendName) {
        createNotification(
                userId,
                NotificationEntity.NotificationType.FRIEND_ACCEPTED,
                "Đã chấp nhận kết bạn",
                friendName + " đã chấp nhận lời mời kết bạn của bạn",
                friendId,
                null, null, null
        );
    }

    public void notifyPostLike(Long postAuthorId, Long likerId, String likerName, Long postId) {
        createNotification(
                postAuthorId,
                NotificationEntity.NotificationType.POST_LIKE,
                "Bài viết được thích",
                likerName + " đã thích bài viết của bạn",
                likerId,
                postId, null, null
        );
    }

    public void notifyPostComment(Long postAuthorId, Long commenterId, String commenterName, Long postId) {
        createNotification(
                postAuthorId,
                NotificationEntity.NotificationType.POST_COMMENT,
                "Bình luận mới",
                commenterName + " đã bình luận bài viết của bạn",
                commenterId,
                postId, null, null
        );
    }

    public void notifyCommentReply(Long commentAuthorId, Long replierId, String replierName, Long postId, Long commentId) {
        createNotification(
                commentAuthorId,
                NotificationEntity.NotificationType.COMMENT_REPLY,
                "Trả lời bình luận",
                replierName + " đã trả lời bình luận của bạn",
                replierId,
                postId, commentId, null
        );
    }

    public void notifyPostModeration(Long postAuthorId, Long moderatorId, Long postId,
                                     String action, String reasonCode, String reasonDetail) {
        String title = "Bài viết của bạn đã bị xử lý";
        String content = "Bài viết đã bị " + action + ". Lý do: " + reasonCode;
        if (reasonDetail != null && !reasonDetail.trim().isEmpty()) {
            content += " - " + reasonDetail.trim();
        }

        createNotification(
                postAuthorId,
                NotificationEntity.NotificationType.POST_MODERATION,
                title,
                content,
                moderatorId,
                postId,
                null,
                null
        );
    }

    public void notifyMessage(Long receiverId, Long senderId, String senderName, Long conversationId, 
                              String conversationName, ConversationEntity.ConversationType conversationType) {
        String title;
        String content;
        
        if (conversationType == ConversationEntity.ConversationType.GROUP) {
            // Tin nhắn nhóm: hiển thị tên nhóm
            title = "Tin nhắn mới";
            content = "Có tin nhắn mới tới nhóm \"" + (conversationName != null && !conversationName.isEmpty() 
                    ? conversationName : "Nhóm chat") + "\"";
        } else {
            // Tin nhắn cá nhân: hiển thị tên người gửi
            title = "Tin nhắn mới";
            content = senderName + " đã gửi tin nhắn";
        }
        
        createNotification(
                receiverId,
                NotificationEntity.NotificationType.MESSAGE,
                title,
                content,
                senderId,
                null, null, conversationId
        );
    }

    public void notifyFollow(Long followeeId, Long followerId, String followerName) {
        createNotification(
                followeeId,
                NotificationEntity.NotificationType.FOLLOW,
                "Người dùng mới theo dõi",
                followerName + " đã theo dõi bạn",
                followerId,
                null, null, null
        );
    }

    private NotificationResponse mapToNotificationResponse(NotificationEntity entity) {
        UserEntity relatedUser = null;
        if (entity.getRelatedUserId() != null) {
            relatedUser = userRepository.findById(entity.getRelatedUserId()).orElse(null);
        }

        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .title(entity.getTitle())
                .content(entity.getContent())
                .relatedUserId(entity.getRelatedUserId())
                .relatedUserName(relatedUser != null ? relatedUser.getUsername() : null)
                .relatedUserAvatar(relatedUser != null ? relatedUser.getAvatar() : null)
                .relatedPostId(entity.getRelatedPostId())
                .relatedCommentId(entity.getRelatedCommentId())
                .relatedConversationId(entity.getRelatedConversationId())
                .relatedContestId(entity.getRelatedContestId())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

