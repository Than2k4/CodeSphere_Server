package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.PostViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostViewEntity, Long> {
    
    @Query("SELECT pv FROM PostViewEntity pv WHERE pv.post.id = :postId AND pv.user.id = :userId")
    Optional<PostViewEntity> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(pv) FROM PostViewEntity pv WHERE pv.post.id = :postId")
    Long countViewsByPostId(@Param("postId") Long postId);
}

