package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.PostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Long> {
    
    @Query("SELECT pl FROM PostLikeEntity pl WHERE pl.post.id = :postId AND pl.user.id = :userId")
    Optional<PostLikeEntity> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(pl) FROM PostLikeEntity pl WHERE pl.post.id = :postId AND pl.vote = 1")
    Long countUpvotesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(pl) FROM PostLikeEntity pl WHERE pl.post.id = :postId AND pl.vote = -1")
    Long countDownvotesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COALESCE(SUM(pl.vote), 0) FROM PostLikeEntity pl WHERE pl.post.id = :postId")
    Long getTotalVotesByPostId(@Param("postId") Long postId);
}

