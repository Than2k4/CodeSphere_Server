package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
    List<TestCaseEntity> findByProblemId(Long problemId);
    
    @Query("SELECT t FROM TestCaseEntity t WHERE t.problem.id = :problemId AND t.isDeleted = false")
    List<TestCaseEntity> findAllTestCasesByProblemId(@Param("problemId") Long problemId);
}

