package com.hcmute.codesphere_server.security.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UserPrinciple implements UserDetails, OAuth2User {

    private final String userId;   // id của User
    private final String email;    // email login

    @JsonIgnore
    private final String password; // passwordHash từ Account

    private final Collection<? extends GrantedAuthority> authorities;

    private Map<String, Object> attributes;

    public UserPrinciple(String userId,
                         String email,
                         String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    // ✅ Build từ AccountEntity (ĐÚNG với entity hiện tại)
    public static UserPrinciple build(AccountEntity account) {
        UserEntity user = account.getUser();

        // Xử lý role
        String roleName = account.getRole() != null 
            ? account.getRole().getName() 
            : "ROLE_USER";
        
        // Đảm bảo có prefix ROLE_ nếu chưa có
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName.toUpperCase();
        }

        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roleName));

        return new UserPrinciple(
                user.getId().toString(),
                account.getEmail(),
                account.getPassword() != null ? account.getPassword() : "", // OAuth2 có thể null
                authorities
        );
    }

    // Cho OAuth2
    public static UserPrinciple create(AccountEntity account, Map<String, Object> attributes) {
        UserPrinciple principal = UserPrinciple.build(account);
        principal.setAttributes(attributes);
        return principal;
    }

    // =============== UserDetails ===============

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Spring dùng cái này làm "username" đăng nhập
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Có thể check account.getIsBlocked() nếu cần
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Có thể check !account.getIsDeleted() nếu cần
    }

    // =============== OAuth2User ===============

    @Override
    public String getName() {
        return email; // hoặc user.getUsername() nếu cần
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    // Getter thêm
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
