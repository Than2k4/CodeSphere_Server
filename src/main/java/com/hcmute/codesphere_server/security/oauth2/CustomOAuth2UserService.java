package com.hcmute.codesphere_server.security.oauth2;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.RoleEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import com.hcmute.codesphere_server.repository.common.RoleRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Google không trả về email");
        }

        AccountEntity account = accountRepository.findByEmail(email).orElse(null);

        if (account == null) {
            // Tự động tạo User + Account từ Google
            account = createAccountFromGoogle(oAuth2User, email);
        }

        return UserPrinciple.create(account, oAuth2User.getAttributes());
    }

    private AccountEntity createAccountFromGoogle(OAuth2User oAuth2User, String email) {
        // Tạo User
        UserEntity user = new UserEntity();
        user.setUsername(oAuth2User.getAttribute("name"));
        user.setAvatar(oAuth2User.getAttribute("picture") != null 
            ? oAuth2User.getAttribute("picture").toString() 
            : "https://res.cloudinary.com/dcti265mg/image/upload/v1728960991/453178253_471506465671661_2781666950760530985_n.png_ewlm3k.png");
        user.setStatus(true);
        user.setIsDeleted(false);
        user.setLastOnline(Instant.now());
        user = userRepository.save(user);

        // Lấy role mặc định ROLE_USER
        RoleEntity role = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> {
                RoleEntity newRole = new RoleEntity();
                newRole.setName("ROLE_USER");
                return roleRepository.save(newRole);
            });

        // Tạo Account
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setEmail(email);
        account.setPassword(null); // OAuth2 không có password
        account.setRole(role);
        account.setIsBlocked(false);
        account.setAuthenWith(1); // 1 = Google
        account.setIsDeleted(false);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());

        return accountRepository.save(account);
    }
}
