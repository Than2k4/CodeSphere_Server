package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.CommentLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, Long> {
    
    @Query("SELECT cl FROM CommentLikeEntity cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
    Optional<CommentLikeEntity> findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(cl) FROM CommentLikeEntity cl WHERE cl.comment.id = :commentId AND cl.vote = 1")
    Long countUpvotesByCommentId(@Param("commentId") Long commentId);
    
    @Query("SELECT COUNT(cl) FROM CommentLikeEntity cl WHERE cl.comment.id = :commentId AND cl.vote = -1")
    Long countDownvotesByCommentId(@Param("commentId") Long commentId);
    
    @Query("SELECT COALESCE(SUM(cl.vote), 0) FROM CommentLikeEntity cl WHERE cl.comment.id = :commentId")
    Long getTotalVotesByCommentId(@Param("commentId") Long commentId);
}

