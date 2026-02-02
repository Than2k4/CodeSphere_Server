package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.FriendRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "friend_requests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}),
       indexes = {
           @Index(name = "idx_friend_request_sender", columnList = "sender_id"),
           @Index(name = "idx_friend_request_receiver", columnList = "receiver_id"),
           @Index(name = "idx_friend_request_status", columnList = "status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}

