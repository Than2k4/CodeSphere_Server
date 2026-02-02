package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long>, JpaSpecificationExecutor<AccountEntity> {
    Optional<AccountEntity> findByEmail(String email);

    @Query("SELECT u FROM AccountEntity u WHERE u.email=:email")
    AccountEntity findAccountByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByUser(com.hcmute.codesphere_server.model.entity.UserEntity user);
}
