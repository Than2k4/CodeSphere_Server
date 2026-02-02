package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ProblemBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemBookmarkRepository extends JpaRepository<ProblemBookmarkEntity, Long> {
    
    /**
     * Tìm bookmark của một user cho một problem
     */
    @Query("SELECT pb FROM ProblemBookmarkEntity pb " +
           "WHERE pb.user.id = :userId AND pb.problem.id = :problemId AND pb.isDeleted = false")
    Optional<ProblemBookmarkEntity> findByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * Kiểm tra user đã bookmark problem chưa
     */
    @Query("SELECT COUNT(pb) > 0 FROM ProblemBookmarkEntity pb " +
           "WHERE pb.user.id = :userId AND pb.problem.id = :problemId AND pb.isDeleted = false")
    boolean existsByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * Lấy tất cả problem IDs mà user đã bookmark
     */
    @Query("SELECT pb.problem.id FROM ProblemBookmarkEntity pb " +
           "WHERE pb.user.id = :userId AND pb.isDeleted = false")
    List<Long> findBookmarkedProblemIdsByUserId(@Param("userId") Long userId);
    
    /**
     * Đếm số bookmark của một user
     */
    @Query("SELECT COUNT(pb) FROM ProblemBookmarkEntity pb " +
           "WHERE pb.user.id = :userId AND pb.isDeleted = false")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * Tìm bookmark của một user cho một problem (kể cả đã bị soft delete)
     * Dùng để kiểm tra duplicate constraint
     */
    @Query("SELECT pb FROM ProblemBookmarkEntity pb " +
           "WHERE pb.user.id = :userId AND pb.problem.id = :problemId")
    Optional<ProblemBookmarkEntity> findByUserIdAndProblemIdIgnoreDeleted(
            @Param("userId") Long userId, 
            @Param("problemId") Long problemId);
}

