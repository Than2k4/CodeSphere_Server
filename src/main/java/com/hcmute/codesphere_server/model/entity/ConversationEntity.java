package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_type", columnList = "type"),
    @Index(name = "idx_conversation_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConversationType type = ConversationType.DIRECT;

    @Column(length = 200)
    private String name; // Cho GROUP conversation

    @Column(length = 500)
    private String avatar; // Cho GROUP conversation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    // Removed @ManyToMany - use ConversationParticipantEntity instead
    // This prevents Hibernate from auto-inserting into conversation_participants
    // without joined_at and role fields

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public enum ConversationType {
        DIRECT,
        GROUP
    }
}

