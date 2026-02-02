package com.hcmute.codesphere_server.model.entity;

import com.hcmute.codesphere_server.model.enums.VerificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "verification_codes")
public class VerificationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 4)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationType type;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean consumed = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // Dữ liệu bổ sung cho đăng ký (username, password hash)
    private String username;

    private String passwordHash;
}

