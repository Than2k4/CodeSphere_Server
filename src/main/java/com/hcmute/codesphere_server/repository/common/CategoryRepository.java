package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}

