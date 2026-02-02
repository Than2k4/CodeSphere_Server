package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<LanguageEntity, Long> {
    Optional<LanguageEntity> findByCode(String code);
    boolean existsByCode(String code);
}

