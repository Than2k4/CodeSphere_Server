package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ContestRegistrationEntity;
import com.hcmute.codesphere_server.model.entity.embedded.ContestRegistrationKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRegistrationRepository extends JpaRepository<ContestRegistrationEntity, ContestRegistrationKey> {

    @Query("SELECT cr FROM ContestRegistrationEntity cr WHERE cr.contest.id = :contestId")
    List<ContestRegistrationEntity> findByContestId(@Param("contestId") Long contestId);

    @Query("SELECT cr FROM ContestRegistrationEntity cr WHERE cr.user.id = :userId")
    List<ContestRegistrationEntity> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cr) > 0 FROM ContestRegistrationEntity cr WHERE cr.contest.id = :contestId AND cr.user.id = :userId")
    boolean existsByContestIdAndUserId(@Param("contestId") Long contestId, @Param("userId") Long userId);
}

