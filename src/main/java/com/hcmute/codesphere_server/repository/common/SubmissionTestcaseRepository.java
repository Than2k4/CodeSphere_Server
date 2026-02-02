package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.SubmissionTestcaseEntity;
import com.hcmute.codesphere_server.model.entity.embedded.SubmissionTestcaseKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionTestcaseRepository extends JpaRepository<SubmissionTestcaseEntity, SubmissionTestcaseKey> {
    
    @Query("SELECT st FROM SubmissionTestcaseEntity st WHERE st.id.submissionId = :submissionId AND st.isDeleted = false")
    List<SubmissionTestcaseEntity> findBySubmissionId(@Param("submissionId") Long submissionId);
}

