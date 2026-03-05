package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.security.JwtService;
import com.lcyhz.urbanova.service.AuthService;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
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
        user.setPhone(request.getPhone());
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
    public UserProfileVo getCurrentUser(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        return toUserProfile(user);
    }

    private AuthPayload buildAuthPayload(UserEntity user) {
        String accessToken = jwtService.generateToken(user.getUserId(), user.getRole(), user.getEmail());
        AuthPayload payload = new AuthPayload();
        payload.setAccessToken(accessToken);
        payload.setTokenType("Bearer");
        payload.setExpiresInSeconds(jwtService.getExpirationSeconds());
        payload.setUser(toUserProfile(user));
        return payload;
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
}

