package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.PostReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface PostReportRepository extends JpaRepository<PostReportEntity, Long> {

    @Query("SELECT COUNT(pr) > 0 FROM PostReportEntity pr WHERE pr.post.id = :postId AND pr.reporter.id = :reporterId")
    boolean existsByPostIdAndReporterId(@Param("postId") Long postId, @Param("reporterId") Long reporterId);

    @Query("SELECT COUNT(pr) FROM PostReportEntity pr WHERE pr.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(pr) FROM PostReportEntity pr WHERE pr.createdAt >= :fromTime AND pr.createdAt < :toTime")
    long countByCreatedAtBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
