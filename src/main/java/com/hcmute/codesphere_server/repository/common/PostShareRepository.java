package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.PostShareEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostShareRepository extends JpaRepository<PostShareEntity, Long> {

    @Query("SELECT ps FROM PostShareEntity ps WHERE ps.post.id = :postId AND ps.user.id = :userId")
    Optional<PostShareEntity> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("SELECT ps FROM PostShareEntity ps WHERE ps.post.id = :postId ORDER BY ps.createdAt DESC")
    List<PostShareEntity> findTopSharesByPostId(@Param("postId") Long postId, Pageable pageable);
}
