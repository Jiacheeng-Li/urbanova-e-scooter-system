package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.entity.AuthSessionEntity;
import com.lcyhz.urbanova.entity.EmailVerificationCodeEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.AuthSessionMapper;
import com.lcyhz.urbanova.mapper.EmailVerificationCodeMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.security.JwtService;
import com.lcyhz.urbanova.service.AuthService;
import com.lcyhz.urbanova.service.support.EmailDeliveryService;
import com.lcyhz.urbanova.service.support.UserAgeSupport;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthSessionMapper authSessionMapper;
    private final EmailVerificationCodeMapper emailVerificationCodeMapper;
    private final EmailDeliveryService emailDeliveryService;

    @Value("${app.jwt.refresh-expiration-days:14}")
    private long refreshExpirationDays;

    @Value("${app.auth.email-verification-expiration-minutes:10}")
    private long emailVerificationExpirationMinutes;

    @Value("${app.auth.email-verification-resend-cooldown-seconds:60}")
    private long emailVerificationResendCooldownSeconds;

    @Value("${app.auth.password-reset-expiration-minutes:10}")
    private long passwordResetExpirationMinutes;

    @Value("${app.auth.password-reset-resend-cooldown-seconds:60}")
    private long passwordResetResendCooldownSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendRegistrationVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "email is required");
        }
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Email already registered");
        }

        LocalDateTime expiresAt = createAndSendVerificationCode(
                normalizedEmail,
                PURPOSE_REGISTER,
                emailVerificationExpirationMinutes,
                emailVerificationResendCooldownSeconds,
                "Verification code was sent recently. Please wait before requesting another one.",
                (targetEmail, code, expiry) -> emailDeliveryService.sendRegistrationVerificationCode(targetEmail, code, expiry));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", normalizedEmail);
        data.put("sent", true);
        data.put("expiresAt", expiresAt);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verifyRegistrationVerificationCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        EmailVerificationCodeEntity latest = verifyCode(normalizedEmail, PURPOSE_REGISTER, code,
                ErrorCodes.EMAIL_VERIFICATION_INVALID,
                "Verification code is invalid or expired",
                "Verification code is no longer available",
                "Verification code is invalid");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", normalizedEmail);
        data.put("verified", true);
        data.put("verifiedAt", latest.getVerifiedAt());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthPayload register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Email already registered");
        }

        EmailVerificationCodeEntity verification = latestVerification(normalizedEmail, PURPOSE_REGISTER);
        if (verification == null || verification.getVerifiedAt() == null
                || verification.getExpiresAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())
                || (verification.getConsumed() != null && verification.getConsumed() == 1)) {
            throw new BusinessException(HttpStatus.PRECONDITION_FAILED.value(), ErrorCodes.EMAIL_VERIFICATION_REQUIRED,
                    "Email verification is required before registration");
        }

        UserEntity user = new UserEntity();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setRole(DomainConstants.ROLE_CUSTOMER);
        user.setDiscountCategory(DomainConstants.DISCOUNT_NONE);
        user.setAccountStatus(DomainConstants.ACCOUNT_ACTIVE);
        user.setBirthDate(normalizeBirthDate(request.getBirthDate()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        verification.setConsumed(1);
        verification.setUpdatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.updateById(verification);

        return buildAuthPayload(user);
    }

    @Override
    public AuthPayload login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), ErrorCodes.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
        }
        if (!DomainConstants.ACCOUNT_ACTIVE.equals(user.getAccountStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "Account is not active");
        }
        return buildAuthPayload(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthPayload refresh(String refreshToken) {
        AuthSessionEntity session = requireRefreshSession(refreshToken);
        UserEntity user = requireActiveUser(session.getUserId());

        session.setRevoked(1);
        session.setUpdatedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
        return buildAuthPayload(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> logout(String userId, String refreshToken) {
        int revoked = 0;
        if (refreshToken != null && !refreshToken.isBlank()) {
            AuthSessionEntity session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSessionEntity>()
                    .eq(AuthSessionEntity::getRefreshToken, refreshToken.trim())
                    .eq(AuthSessionEntity::getUserId, userId));
            if (session != null && (session.getRevoked() == null || session.getRevoked() == 0)) {
                session.setRevoked(1);
                session.setUpdatedAt(LocalDateTime.now());
                authSessionMapper.updateById(session);
                revoked = 1;
            }
        } else {
            for (AuthSessionEntity session : authSessionMapper.selectList(new LambdaQueryWrapper<AuthSessionEntity>()
                    .eq(AuthSessionEntity::getUserId, userId)
                    .eq(AuthSessionEntity::getRevoked, 0))) {
                session.setRevoked(1);
                session.setUpdatedAt(LocalDateTime.now());
                authSessionMapper.updateById(session);
                revoked++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("revokedSessions", revoked);
        data.put("loggedOut", true);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accepted", true);
        data.put("email", normalizedEmail);
        if (user == null) {
            return data;
        }

        EmailVerificationCodeEntity latest = latestVerification(normalizedEmail, PURPOSE_PASSWORD_RESET);
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(passwordResetResendCooldownSeconds).isAfter(LocalDateTime.now())) {
            return data;
        }

        createAndSendVerificationCode(
                normalizedEmail,
                PURPOSE_PASSWORD_RESET,
                passwordResetExpirationMinutes,
                passwordResetResendCooldownSeconds,
                "Password reset code was sent recently. Please wait before requesting another one.",
                (targetEmail, code, expiresAt) -> emailDeliveryService.sendPasswordResetCode(targetEmail, code, expiresAt));
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        EmailVerificationCodeEntity verification = verifyCode(normalizedEmail, PURPOSE_PASSWORD_RESET, code,
                ErrorCodes.PASSWORD_RESET_CODE_INVALID,
                "Password reset code is invalid or expired",
                "Password reset code is no longer available",
                "Password reset code is invalid");

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));
        if (user == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.PASSWORD_RESET_CODE_INVALID,
                    "Password reset code is invalid or expired");
        }
        user = requireActiveUser(user.getUserId());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        verification.setConsumed(1);
        verification.setUpdatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.updateById(verification);

        for (AuthSessionEntity session : authSessionMapper.selectList(new LambdaQueryWrapper<AuthSessionEntity>()
                .eq(AuthSessionEntity::getUserId, user.getUserId())
                .eq(AuthSessionEntity::getRevoked, 0))) {
            session.setRevoked(1);
            session.setUpdatedAt(LocalDateTime.now());
            authSessionMapper.updateById(session);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("passwordReset", true);
        return data;
    }

    @Override
    public UserProfileVo getCurrentUser(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        return toUserProfile(user);
    }

    private AuthPayload buildAuthPayload(UserEntity user) {
        AuthSessionEntity session = createSession(user.getUserId());
        String accessToken = jwtService.generateToken(user.getUserId(), user.getRole(), user.getEmail());
        AuthPayload payload = new AuthPayload();
        payload.setSessionId(session.getSessionId());
        payload.setAccessToken(accessToken);
        payload.setRefreshToken(session.getRefreshToken());
        payload.setTokenType("Bearer");
        payload.setExpiresInSeconds(jwtService.getExpirationSeconds());
        payload.setRefreshExpiresInSeconds(refreshExpirationDays * 24 * 60 * 60);
        payload.setUser(toUserProfile(user));
        return payload;
    }

    private AuthSessionEntity createSession(String userId) {
        AuthSessionEntity session = new AuthSessionEntity();
        session.setSessionId("SES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        session.setUserId(userId);
        session.setRefreshToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        session.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        session.setRevoked(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        authSessionMapper.insert(session);
        return session;
    }

    private AuthSessionEntity requireRefreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "refreshToken is required");
        }
        AuthSessionEntity session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSessionEntity>()
                .eq(AuthSessionEntity::getRefreshToken, refreshToken.trim())
                .eq(AuthSessionEntity::getRevoked, 0));
        if (session == null || session.getExpiresAt() == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), ErrorCodes.AUTH_INVALID_CREDENTIALS, "Refresh token is invalid or expired");
        }
        return session;
    }

    private UserEntity requireActiveUser(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        if (!DomainConstants.ACCOUNT_ACTIVE.equals(user.getAccountStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "Account is not active");
        }
        return user;
    }

    private UserProfileVo toUserProfile(UserEntity user) {
        UserProfileVo profileVo = new UserProfileVo();
        profileVo.setUserId(user.getUserId());
        profileVo.setEmail(user.getEmail());
        profileVo.setFullName(user.getFullName());
        profileVo.setPhone(user.getPhone());
        profileVo.setRole(user.getRole());
        profileVo.setDiscountCategory(user.getDiscountCategory());
        profileVo.setAccountStatus(user.getAccountStatus());
        profileVo.setBirthDate(user.getBirthDate());
        profileVo.setAge(UserAgeSupport.resolveAge(user.getBirthDate()));
        profileVo.setAgeGroup(UserAgeSupport.resolveAgeGroup(user.getBirthDate()));
        profileVo.setCreatedAt(user.getCreatedAt());
        return profileVo;
    }

    private EmailVerificationCodeEntity latestVerification(String normalizedEmail, String purpose) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return null;
        }
        return emailVerificationCodeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCodeEntity>()
                .eq(EmailVerificationCodeEntity::getEmail, normalizedEmail)
                .eq(EmailVerificationCodeEntity::getPurpose, purpose)
                .orderByDesc(EmailVerificationCodeEntity::getCreatedAt)
                .last("LIMIT 1"));
    }

    private LocalDateTime createAndSendVerificationCode(String email,
                                                        String purpose,
                                                        long expirationMinutes,
                                                        long resendCooldownSeconds,
                                                        String resendCooldownMessage,
                                                        VerificationCodeSender sender) {
        EmailVerificationCodeEntity latest = latestVerification(email, purpose);
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCodes.PRECONDITION_FAILED,
                    resendCooldownMessage);
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setVerificationCodeId("EMV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        entity.setEmail(email);
        entity.setPurpose(purpose);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(expiresAt);
        entity.setVerifiedAt(null);
        entity.setConsumed(0);
        entity.setAttemptCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.insert(entity);

        sender.send(email, code, expiresAt);
        return expiresAt;
    }

    private EmailVerificationCodeEntity verifyCode(String email,
                                                   String purpose,
                                                   String code,
                                                   String errorCode,
                                                   String expiredMessage,
                                                   String consumedMessage,
                                                   String mismatchMessage) {
        EmailVerificationCodeEntity latest = latestVerification(email, purpose);
        if (latest == null || latest.getExpiresAt() == null || latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), errorCode, expiredMessage);
        }
        if (latest.getConsumed() != null && latest.getConsumed() == 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), errorCode, consumedMessage);
        }
        if (!passwordEncoder.matches(String.valueOf(code).trim(), latest.getCodeHash())) {
            latest.setAttemptCount((latest.getAttemptCount() == null ? 0 : latest.getAttemptCount()) + 1);
            if (latest.getAttemptCount() >= 5) {
                latest.setConsumed(1);
            }
            latest.setUpdatedAt(LocalDateTime.now());
            emailVerificationCodeMapper.updateById(latest);
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), errorCode, mismatchMessage);
        }
        latest.setVerifiedAt(LocalDateTime.now());
        latest.setUpdatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.updateById(latest);
        return latest;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDate normalizeBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "birthDate must not be in the future");
        }
        return birthDate;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @FunctionalInterface
    private interface VerificationCodeSender {
        void send(String email, String code, LocalDateTime expiresAt);
    }
}
