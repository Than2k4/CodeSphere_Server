package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    
    @Query("SELECT c FROM CommentEntity c WHERE c.id = :id")
    Optional<CommentEntity> findById(@Param("id") Long id);
    
    @Query("SELECT c FROM CommentEntity c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<CommentEntity> findTopLevelCommentsByPostId(@Param("postId") Long postId, Pageable pageable);
    
    @Query("SELECT c FROM CommentEntity c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<CommentEntity> findRepliesByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);
    
    @Query("SELECT c FROM CommentEntity c WHERE c.post.id = :postId")
    List<CommentEntity> findAllByPostId(@Param("postId") Long postId);
    
    // Methods for Problem comments
    @Query("SELECT c FROM CommentEntity c WHERE c.problem.id = :problemId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<CommentEntity> findTopLevelCommentsByProblemId(@Param("problemId") Long problemId, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.problem.id = :problemId")
    Long countByProblemId(@Param("problemId") Long problemId);
    
    @Query("SELECT c FROM CommentEntity c WHERE c.problem.id = :problemId")
    List<CommentEntity> findAllByProblemId(@Param("problemId") Long problemId);
}

