package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<ProblemEntity, Long>, JpaSpecificationExecutor<ProblemEntity> {
    Optional<ProblemEntity> findByCode(String code);
    boolean existsByCode(String code);
    
    @Query("SELECT p FROM ProblemEntity p WHERE p.id = :id AND p.status = true")
    Optional<ProblemEntity> findByIdAndStatusTrue(@Param("id") Long id);
}

