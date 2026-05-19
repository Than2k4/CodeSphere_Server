package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.SubmissionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Query(value = "SELECT s.user_id, u.username, " +
           "COUNT(DISTINCT s.problem_id) as totalSolved, " +
           "COUNT(DISTINCT CASE WHEN p.level = 'EASY' THEN s.problem_id END) as solvedEasy, " +
           "COUNT(DISTINCT CASE WHEN p.level = 'MEDIUM' THEN s.problem_id END) as solvedMedium, " +
           "COUNT(DISTINCT CASE WHEN p.level = 'HARD' THEN s.problem_id END) as solvedHard, " +
           "u.avatar " +
           "FROM submissions s " +
           "INNER JOIN users u ON s.user_id = u.id " +
           "INNER JOIN problems p ON s.problem_id = p.id " +
           "WHERE s.is_deleted = false " +
           "AND s.is_accepted = true " +
           "AND s.created_at >= :fromDate " +
           "GROUP BY s.user_id, u.username, u.avatar " +
           "ORDER BY totalSolved DESC, s.user_id ASC", nativeQuery = true)
    java.util.List<Object[]> findUsersWithSolvedCountSince(@Param("fromDate") Instant fromDate);
}

