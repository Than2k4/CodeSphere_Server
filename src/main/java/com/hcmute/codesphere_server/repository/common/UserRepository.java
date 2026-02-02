package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    @Query("SELECT u FROM UserEntity u WHERE u.isDeleted = false AND u.status = true AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<UserEntity> searchUsers(@Param("query") String query, Pageable pageable);
}
