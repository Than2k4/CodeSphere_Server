package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.RoleEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.response.UserManagementResponse;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import com.hcmute.codesphere_server.repository.common.RoleRepository;
import com.hcmute.codesphere_server.repository.common.SubmissionRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final SubmissionRepository submissionRepository;

    public Page<UserManagementResponse> getUsers(Pageable pageable, String search, String role, String status) {
        Specification<UserEntity> spec = Specification.where(null);

        // Filter by search (username)
        if (search != null && !search.trim().isEmpty()) {
            Specification<UserEntity> searchSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%");
            spec = spec.and(searchSpec);
        }

        // Filter by status
        if (status != null && !status.trim().isEmpty()) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                Specification<UserEntity> statusSpec = (root, query, cb) ->
                        cb.and(
                                cb.equal(root.get("status"), true),
                                cb.equal(root.get("isDeleted"), false)
                        );
                spec = spec.and(statusSpec);
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                Specification<UserEntity> statusSpec = (root, query, cb) ->
                        cb.and(
                                cb.equal(root.get("status"), false),
                                cb.equal(root.get("isDeleted"), false)
                        );
                spec = spec.and(statusSpec);
            }
            // BLOCKED status will be filtered after fetching accounts
        }

        // Filter by isDeleted = false
        Specification<UserEntity> notDeletedSpec = (root, query, cb) ->
                cb.equal(root.get("isDeleted"), false);
        spec = spec.and(notDeletedSpec);

        // Fetch all matching users (we'll filter by account fields after)
        Page<UserEntity> users = userRepository.findAll(spec, pageable);

        // Convert to responses
        List<UserManagementResponse> responses = users.getContent().stream()
                .map(this::mapToUserManagementResponse)
                .filter(response -> {
                    // Filter by blocked status
                    if (status != null && "BLOCKED".equalsIgnoreCase(status) && !response.getIsBlocked()) {
                        return false;
                    }
                    // Filter by role
                    if (role != null && !role.trim().isEmpty() && !response.getRole().equalsIgnoreCase(role)) {
                        return false;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Create a new page with filtered results
        return new org.springframework.data.domain.PageImpl<>(
                responses,
                pageable,
                users.getTotalElements()
        );
    }

    public UserManagementResponse getUserById(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        return mapToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse updateUserStatus(Long userId, String status) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            user.setStatus(true);
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            user.setStatus(false);
        } else {
            throw new RuntimeException("Status không hợp lệ. Phải là ACTIVE hoặc INACTIVE");
        }

        user = userRepository.save(user);
        return mapToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse blockUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        AccountEntity account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account không tồn tại"));

        account.setIsBlocked(true);
        accountRepository.save(account);

        return mapToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse unblockUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        AccountEntity account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account không tồn tại"));

        account.setIsBlocked(false);
        accountRepository.save(account);

        return mapToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse changeUserRole(Long userId, String roleName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        AccountEntity account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account không tồn tại"));

        account.setRole(role);
        accountRepository.save(account);

        return mapToUserManagementResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("User đã bị xóa");
        }

        // Soft delete
        user.setIsDeleted(true);
        user.setStatus(false);
        userRepository.save(user);

        // Also soft delete account
        Optional<AccountEntity> accountOpt = accountRepository.findByUser(user);
        if (accountOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            account.setIsDeleted(true);
            accountRepository.save(account);
        }
    }

    private UserManagementResponse mapToUserManagementResponse(UserEntity user) {
        AccountEntity account = accountRepository.findByUser(user).orElse(null);

        String roleName = "USER";
        String statusStr = "INACTIVE";
        Boolean isBlocked = false;
        String email = null;

        if (account != null) {
            if (account.getRole() != null) {
                roleName = account.getRole().getName();
            }
            isBlocked = account.getIsBlocked();
            email = account.getEmail();
        }

        if (user.getStatus() != null && user.getStatus()) {
            statusStr = isBlocked ? "BLOCKED" : "ACTIVE";
        } else {
            statusStr = "INACTIVE";
        }

        Long totalSubmissions = submissionRepository.countSubmissionsByUserId(user.getId());
        Long totalSolved = submissionRepository.countAcceptedSubmissionsByUserId(user.getId());

        return UserManagementResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(email)
                .role(roleName)
                .status(statusStr)
                .isBlocked(isBlocked)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastOnline())
                .totalSubmissions(totalSubmissions)
                .totalSolved(totalSolved)
                .build();
    }
}

