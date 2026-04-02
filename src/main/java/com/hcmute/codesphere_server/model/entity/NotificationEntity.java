package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user; // Người nhận notification

    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50) NOT NULL")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    private Long relatedUserId; // User liên quan (người like, comment, etc.)

    private Long relatedPostId; // Post liên quan

    private Long relatedCommentId; // Comment liên quan

    private Long relatedConversationId; // Conversation liên quan

    private Long relatedContestId; // Contest liên quan

    @Column(nullable = false)
    private Boolean isRead = false;

    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum NotificationType {
        FOLLOW,
        FRIEND_REQUEST,
        FRIEND_ACCEPTED,
        POST_LIKE,
        POST_COMMENT,
        COMMENT_REPLY,
        POST_MODERATION,
        MESSAGE,
        CONTEST_REMINDER
    }
}

