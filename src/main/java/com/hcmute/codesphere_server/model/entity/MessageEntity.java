package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_conversation", columnList = "conversation_id"),
    @Index(name = "idx_message_sender", columnList = "sender_id"),
    @Index(name = "idx_message_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content; // Nullable nếu là IMAGE

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @Column(length = 500)
    private String imageUrl; // Cho IMAGE type

    @Column(length = 500)
    private String fileUrl; // Cho FILE type

    @Column(length = 100)
    private String fileName; // Tên file gốc

    @Column(length = 50)
    private String fileType; // Loại file (pdf, doc, etc.)

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}

