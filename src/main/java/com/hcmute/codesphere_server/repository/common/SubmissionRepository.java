package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.SubmissionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long>, JpaSpecificationExecutor<SubmissionEntity> {
    
    @Query("SELECT s FROM SubmissionEntity s WHERE s.id = :id AND s.isDeleted = false")
    Optional<SubmissionEntity> findByIdAndNotDeleted(@Param("id") Long id);
    
    @Query("SELECT s FROM SubmissionEntity s WHERE s.user.id = :userId AND s.isDeleted = false")
    Page<SubmissionEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT s FROM SubmissionEntity s WHERE s.problem.id = :problemId AND s.isDeleted = false")
    Page<SubmissionEntity> findByProblemId(@Param("problemId") Long problemId, Pageable pageable);
    
    @Query("SELECT s FROM SubmissionEntity s WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.isDeleted = false")
    Page<SubmissionEntity> findByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId, Pageable pageable);
    
    // Lấy tất cả submissions của một problem (chưa bị xóa)
    @Query("SELECT s FROM SubmissionEntity s WHERE s.problem.id = :problemId AND s.isDeleted = false")
    java.util.List<SubmissionEntity> findAllByProblemId(@Param("problemId") Long problemId);
    
    // Đếm tổng số submissions của một user cho một problem
    @Query("SELECT COUNT(s) FROM SubmissionEntity s " +
           "WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.isDeleted = false")
    Long countSubmissionsByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    // Đếm tổng số submissions của một user
    @Query("SELECT COUNT(s) FROM SubmissionEntity s " +
           "WHERE s.user.id = :userId AND s.isDeleted = false")
    Long countSubmissionsByUserId(@Param("userId") Long userId);
    
    // Đếm tổng số accepted submissions của một user
    @Query("SELECT COUNT(s) FROM SubmissionEntity s " +
           "WHERE s.user.id = :userId AND s.isAccepted = true AND s.isDeleted = false")
    Long countAcceptedSubmissionsByUserId(@Param("userId") Long userId);
    
    // Đếm số bài tập đã thử của một user (distinct problem IDs)
    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM SubmissionEntity s " +
           "WHERE s.user.id = :userId AND s.isDeleted = false")
    Long countDistinctProblemsAttemptedByUserId(@Param("userId") Long userId);
    
    // Đếm số bài tập đã thử theo độ khó của một user
    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM SubmissionEntity s " +
           "WHERE s.user.id = :userId AND s.problem.level = :level AND s.isDeleted = false")
    Long countDistinctProblemsAttemptedByUserIdAndLevel(@Param("userId") Long userId, @Param("level") String level);
    
    // Đếm tổng số submissions của một problem
    @Query("SELECT COUNT(s) FROM SubmissionEntity s " +
           "WHERE s.problem.id = :problemId AND s.isDeleted = false")
    Long countSubmissionsByProblemId(@Param("problemId") Long problemId);
    
    // Đếm tổng số accepted submissions của một problem
    @Query("SELECT COUNT(s) FROM SubmissionEntity s " +
           "WHERE s.problem.id = :problemId AND s.isAccepted = true AND s.isDeleted = false")
    Long countAcceptedSubmissionsByProblemId(@Param("problemId") Long problemId);
    
    // Đếm số user đã thử một problem (distinct user IDs)
    @Query("SELECT COUNT(DISTINCT s.user.id) FROM SubmissionEntity s " +
           "WHERE s.problem.id = :problemId AND s.isDeleted = false")
    Long countDistinctUsersAttemptedByProblemId(@Param("problemId") Long problemId);
    
    // Đếm số user đã giải đúng một problem (distinct user IDs với accepted submissions)
    @Query("SELECT COUNT(DISTINCT s.user.id) FROM SubmissionEntity s " +
           "WHERE s.problem.id = :problemId AND s.isAccepted = true AND s.isDeleted = false")
    Long countDistinctUsersSolvedByProblemId(@Param("problemId") Long problemId);
}

