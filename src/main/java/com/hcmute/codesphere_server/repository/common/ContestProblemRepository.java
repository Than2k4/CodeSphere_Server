package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ContestProblemEntity;
import com.hcmute.codesphere_server.model.entity.embedded.ContestProblemKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblemEntity, ContestProblemKey> {

    @Query("SELECT cp FROM ContestProblemEntity cp WHERE cp.contest.id = :contestId ORDER BY cp.problemOrder ASC")
    List<ContestProblemEntity> findByContestIdOrderByProblemOrder(@Param("contestId") Long contestId);

    @Query("SELECT cp FROM ContestProblemEntity cp WHERE cp.contest.id = :contestId AND cp.problem.id = :problemId")
    Optional<ContestProblemEntity> findByContestIdAndProblemId(@Param("contestId") Long contestId, @Param("problemId") Long problemId);
    
    @Query("SELECT cp FROM ContestProblemEntity cp WHERE cp.problem.id = :problemId")
    List<ContestProblemEntity> findByProblemId(@Param("problemId") Long problemId);
}

