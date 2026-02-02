package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, JpaSpecificationExecutor<PostEntity> {
    
    @Query("SELECT p FROM PostEntity p WHERE p.id = :id AND p.isBlocked = false")
    Optional<PostEntity> findByIdAndNotBlocked(@Param("id") Long id);
    
    @Query("SELECT p FROM PostEntity p WHERE p.author.id = :authorId AND p.isBlocked = false")
    Page<PostEntity> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM PostEntity p WHERE p.author.id = :authorId AND p.isBlocked = false")
    Long countByAuthorId(@Param("authorId") Long authorId);
}

