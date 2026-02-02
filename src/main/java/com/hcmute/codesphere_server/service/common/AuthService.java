package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.RoleEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.entity.VerificationCodeEntity;
import com.hcmute.codesphere_server.model.enums.VerificationType;
import com.hcmute.codesphere_server.model.payload.request.*;
import com.hcmute.codesphere_server.model.payload.response.AuthResponse;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import com.hcmute.codesphere_server.repository.common.RoleRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import com.hcmute.codesphere_server.repository.common.VerificationCodeRepository;
import com.hcmute.codesphere_server.security.config.Email.EmailService;
import com.hcmute.codesphere_server.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 4;
    private static final int OTP_EXPIRE_MINUTES = 10;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Tạo User
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername() != null ? request.getUsername() : request.getEmail().split("@")[0]);
        user.setAvatar("https://res.cloudinary.com/dcti265mg/image/upload/v1728960991/453178253_471506465671661_2781666950760530985_n.png_ewlm3k.png");
        user.setStatus(true);
        user.setIsDeleted(false);
        user.setLastOnline(Instant.now());
        user = userRepository.save(user);

        // Lấy role mặc định
        RoleEntity role = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> {
                RoleEntity newRole = new RoleEntity();
                newRole.setName("ROLE_USER");
                return roleRepository.save(newRole);
            });

        // Tạo Account
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);
        account.setIsBlocked(false);
        account.setAuthenWith(0); // 0 = local
        account.setIsDeleted(false);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        account = accountRepository.save(account);

        // Tạo token
        String token = jwtProvider.generateToken(
            user.getId().toString(),
            account.getEmail(),
            role.getName()
        );

        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .userId(user.getId())
            .email(account.getEmail())
            .username(user.getUsername())
            .role(role.getName())
            .build();
    }

    @Transactional
    public void registerInit(RegisterInitRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        String otp = generateOtp();
        verificationCodeRepository.deleteByEmailAndTypeAndExpiresAtBefore(
                request.getEmail(),
                VerificationType.REGISTER,
                Instant.now()
        );

        VerificationCodeEntity code = new VerificationCodeEntity();
        code.setEmail(request.getEmail());
        code.setUsername(request.getUsername());
        code.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        code.setCode(otp);
        code.setType(VerificationType.REGISTER);
        code.setExpiresAt(Instant.now().plus(OTP_EXPIRE_MINUTES, ChronoUnit.MINUTES));
        code.setConsumed(false);
        code.setCreatedAt(Instant.now());
        code.setUpdatedAt(Instant.now());
        verificationCodeRepository.save(code);

        emailService.sendSimpleEmail(
                request.getEmail(),
                "CodeSphere - OTP đăng ký",
                String.format("Mã OTP của bạn là: %s\nHết hạn sau %d phút.", otp, OTP_EXPIRE_MINUTES)
        );
    }

    @Transactional
    public AuthResponse registerVerify(RegisterVerifyRequest request) {
        VerificationCodeEntity code = verificationCodeRepository
                .findTopByEmailAndTypeAndConsumedFalseOrderByCreatedAtDesc(
                        request.getEmail(),
                        VerificationType.REGISTER
                ).orElseThrow(() -> new RuntimeException("Không tìm thấy OTP đăng ký. Vui lòng gửi lại."));

        if (code.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!code.getCode().equals(request.getOtp())) {
            throw new RuntimeException("OTP không chính xác.");
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng.");
        }

        AuthResponse response = createAccountAfterVerification(
                request.getEmail(),
                code.getUsername(),
                code.getPasswordHash()
        );

        code.setConsumed(true);
        code.setUpdatedAt(Instant.now());
        verificationCodeRepository.save(code);

        return response;
    }

    public AuthResponse login(LoginRequest request) {
        // Kiểm tra email có tồn tại không
        boolean emailExists = accountRepository.existsByEmail(request.getEmail());
        
        try {
            // Xác thực
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Lấy account
            AccountEntity account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            UserEntity user = account.getUser();
            RoleEntity role = account.getRole();

            // Tạo token
            String token = jwtProvider.generateToken(
                user.getId().toString(),
                account.getEmail(),
                role.getName()
            );

            return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(account.getEmail())
                .username(user.getUsername())
                .role(role.getName())
                .build();
        } catch (BadCredentialsException e) {
            // Distinguish between "incorrect account" and "wrong password"
            if (!emailExists) {
                throw new RuntimeException("Account not found. Please check your email.");
            } else {
                throw new RuntimeException("Incorrect password. Please check your password.");
            }
        }
    }

    @Transactional
    public void forgotPasswordInit(ForgotPasswordInitRequest request) {
        AccountEntity account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String otp = generateOtp();
        verificationCodeRepository.deleteByEmailAndTypeAndExpiresAtBefore(
                request.getEmail(),
                VerificationType.RESET_PASSWORD,
                Instant.now()
        );

        VerificationCodeEntity code = new VerificationCodeEntity();
        code.setEmail(account.getEmail());
        code.setCode(otp);
        code.setType(VerificationType.RESET_PASSWORD);
        code.setExpiresAt(Instant.now().plus(OTP_EXPIRE_MINUTES, ChronoUnit.MINUTES));
        code.setConsumed(false);
        code.setCreatedAt(Instant.now());
        code.setUpdatedAt(Instant.now());
        verificationCodeRepository.save(code);

        emailService.sendSimpleEmail(
                account.getEmail(),
                "CodeSphere - OTP đặt lại mật khẩu",
                String.format("Mã OTP đặt lại mật khẩu của bạn là: %s\nHết hạn sau %d phút.", otp, OTP_EXPIRE_MINUTES)
        );
    }

    @Transactional
    public void forgotPasswordVerify(ForgotPasswordVerifyRequest request) {
        VerificationCodeEntity code = verificationCodeRepository
                .findTopByEmailAndTypeAndConsumedFalseOrderByCreatedAtDesc(
                        request.getEmail(),
                        VerificationType.RESET_PASSWORD
                ).orElseThrow(() -> new RuntimeException("Không tìm thấy OTP. Vui lòng gửi lại."));

        if (code.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!code.getCode().equals(request.getOtp())) {
            throw new RuntimeException("OTP không chính xác.");
        }

        AccountEntity account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        code.setConsumed(true);
        code.setUpdatedAt(Instant.now());
        verificationCodeRepository.save(code);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        AccountEntity account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
    }

    private String generateOtp() {
        int number = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", number);
    }

    private AuthResponse createAccountAfterVerification(String email, String username, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setUsername(username != null ? username : email.split("@")[0]);
        user.setAvatar("https://res.cloudinary.com/dcti265mg/image/upload/v1728960991/453178253_471506465671661_2781666950760530985_n.png_ewlm3k.png");
        user.setStatus(true);
        user.setIsDeleted(false);
        user.setLastOnline(Instant.now());
        user = userRepository.save(user);

        RoleEntity role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName("ROLE_USER");
                    return roleRepository.save(newRole);
                });

        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setEmail(email);
        account.setPassword(passwordHash);
        account.setRole(role);
        account.setIsBlocked(false);
        account.setAuthenWith(0);
        account.setIsDeleted(false);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        String token = jwtProvider.generateToken(
                user.getId().toString(),
                account.getEmail(),
                role.getName()
        );

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(account.getEmail())
                .username(user.getUsername())
                .role(role.getName())
                .build();
    }
}

