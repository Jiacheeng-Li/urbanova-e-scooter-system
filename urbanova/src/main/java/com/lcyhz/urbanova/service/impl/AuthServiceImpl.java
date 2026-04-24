package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.entity.AuthSessionEntity;
import com.lcyhz.urbanova.entity.PasswordResetTokenEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.AuthSessionMapper;
import com.lcyhz.urbanova.mapper.PasswordResetTokenMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.security.JwtService;
import com.lcyhz.urbanova.service.AuthService;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthSessionMapper authSessionMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;

    @Value("${app.jwt.refresh-expiration-days:14}")
    private long refreshExpirationDays;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthPayload register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, normalizedEmail));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Email already registered");
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
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

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
            data.put("resetToken", null);
            return data;
        }

        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setResetTokenId("RST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        token.setUserId(user.getUserId());
        token.setResetToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(0);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        passwordResetTokenMapper.insert(token);

        data.put("resetToken", token.getResetToken());
        data.put("expiresAt", token.getExpiresAt());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resetPassword(String resetToken, String newPassword) {
        PasswordResetTokenEntity token = passwordResetTokenMapper.selectOne(new LambdaQueryWrapper<PasswordResetTokenEntity>()
                .eq(PasswordResetTokenEntity::getResetToken, resetToken)
                .eq(PasswordResetTokenEntity::getUsed, 0));
        if (token == null || token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.AUTH_INVALID_CREDENTIALS, "Reset token is invalid or expired");
        }

        UserEntity user = requireActiveUser(token.getUserId());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        token.setUsed(1);
        token.setUpdatedAt(LocalDateTime.now());
        passwordResetTokenMapper.updateById(token);

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
        profileVo.setCreatedAt(user.getCreatedAt());
        return profileVo;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
