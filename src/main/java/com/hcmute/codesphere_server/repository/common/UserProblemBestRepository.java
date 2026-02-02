package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.UserProblemBestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProblemBestRepository extends JpaRepository<UserProblemBestEntity, Long> {
    
    /**
     * Tìm best submission của một user cho một problem
     */
    Optional<UserProblemBestEntity> findByUserIdAndProblemId(Long userId, Long problemId);
    
    /**
     * Lấy tất cả best submissions của một problem, sắp xếp theo điểm cao nhất
     */
    @Query("SELECT upb FROM UserProblemBestEntity upb " +
           "WHERE upb.problem.id = :problemId " +
           "ORDER BY upb.bestScore DESC, upb.bestSubmission.createdAt ASC")
    List<UserProblemBestEntity> findAllByProblemIdOrderByBestScoreDesc(@Param("problemId") Long problemId);
    
    /**
     * Lấy tất cả best submissions của một user (chỉ những bài đã giải đúng)
     */
    @Query("SELECT upb FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true")
    List<UserProblemBestEntity> findAllByUserIdAndAccepted(@Param("userId") Long userId);
    
    /**
     * Đếm số bài đã giải đúng của một user theo độ khó
     */
    @Query("SELECT COUNT(upb) FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true AND upb.problem.level = :level")
    Long countSolvedByUserIdAndLevel(@Param("userId") Long userId, @Param("level") String level);
    
    /**
     * Đếm tổng số bài đã giải đúng của một user
     */
    @Query("SELECT COUNT(upb) FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true")
    Long countSolvedByUserId(@Param("userId") Long userId);
    
    /**
     * Lấy tất cả users với số bài đã giải đúng, group by user và đếm
     * Sắp xếp theo số bài giải đúng giảm dần
     */
    @Query(value = "SELECT upb.user_id, u.username, " +
           "COUNT(*) as totalSolved, " +
           "SUM(CASE WHEN p.level = 'EASY' THEN 1 ELSE 0 END) as solvedEasy, " +
           "SUM(CASE WHEN p.level = 'MEDIUM' THEN 1 ELSE 0 END) as solvedMedium, " +
           "SUM(CASE WHEN p.level = 'HARD' THEN 1 ELSE 0 END) as solvedHard " +
           "FROM user_problem_best upb " +
           "INNER JOIN submissions s ON upb.best_submission_id = s.id " +
           "INNER JOIN users u ON upb.user_id = u.id " +
           "INNER JOIN problems p ON upb.problem_id = p.id " +
           "WHERE s.is_accepted = true " +
           "GROUP BY upb.user_id, u.username " +
           "ORDER BY totalSolved DESC, upb.user_id ASC", nativeQuery = true)
    List<Object[]> findAllUsersWithSolvedCount();
}

