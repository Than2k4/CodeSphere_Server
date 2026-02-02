package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.VerificationCodeEntity;
import com.hcmute.codesphere_server.model.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long> {

    Optional<VerificationCodeEntity> findTopByEmailAndTypeAndConsumedFalseOrderByCreatedAtDesc(String email, VerificationType type);

    void deleteByEmailAndTypeAndExpiresAtBefore(String email, VerificationType type, Instant expiresAt);
}

