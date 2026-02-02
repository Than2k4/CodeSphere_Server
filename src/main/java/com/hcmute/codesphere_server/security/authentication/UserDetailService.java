package com.hcmute.codesphere_server.security.authentication;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {
    private final AccountRepository accountRepository;
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AccountEntity accountModel = accountRepository.findAccountByEmail(email);
        if (accountModel == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
        }

        return UserPrinciple.build(accountModel);
    }
}
