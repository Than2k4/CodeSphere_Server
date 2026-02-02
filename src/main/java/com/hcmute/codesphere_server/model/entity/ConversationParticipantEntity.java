package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.ParticipantRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "conversation_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}),
       indexes = {
           @Index(name = "idx_participant_conversation", columnList = "conversation_id"),
           @Index(name = "idx_participant_user", columnList = "user_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @lombok.Builder.Default
    private ParticipantRole role = ParticipantRole.MEMBER;

    @Column(nullable = false, updatable = false)
    @lombok.Builder.Default
    private Instant joinedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
        if (role == null) {
            role = ParticipantRole.MEMBER;
        }
    }
}

