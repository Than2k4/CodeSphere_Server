package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ContestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends JpaRepository<ContestEntity, Long>, JpaSpecificationExecutor<ContestEntity> {

    List<ContestEntity> findByIsPublic(Boolean isPublic);

    Optional<ContestEntity> findByAccessCode(String accessCode);

    @Query("SELECT c FROM ContestEntity c WHERE c.startTime > :now")
    List<ContestEntity> findUpcoming(@Param("now") Instant now);

    @Query("SELECT c FROM ContestEntity c WHERE c.startTime <= :now AND c.endTime >= :now")
    List<ContestEntity> findOngoing(@Param("now") Instant now);

    @Query("SELECT c FROM ContestEntity c WHERE c.endTime < :now")
    List<ContestEntity> findEnded(@Param("now") Instant now);

    @Query("SELECT c FROM ContestEntity c WHERE c.author.id = :authorId")
    List<ContestEntity> findByAuthorId(@Param("authorId") Long authorId);
}

