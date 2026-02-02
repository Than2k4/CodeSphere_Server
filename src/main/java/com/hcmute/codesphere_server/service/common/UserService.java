package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.response.UserProfileResponse;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        AccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        UserEntity user = account.getUser();
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(account.getEmail())
                .avatar(user.getAvatar())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .status(user.getStatus())
                .lastOnline(user.getLastOnline())
                .role(account.getRole().getName())
                .authenWith(account.getAuthenWith())
                .isBlocked(account.getIsBlocked())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}

