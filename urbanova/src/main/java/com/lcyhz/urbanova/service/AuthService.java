package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;

import java.util.Map;

public interface AuthService {
    AuthPayload register(RegisterRequest request);

    AuthPayload login(LoginRequest request);

    AuthPayload refresh(String refreshToken);

    Map<String, Object> logout(String userId, String refreshToken);

    Map<String, Object> forgotPassword(String email);

    Map<String, Object> resetPassword(String resetToken, String newPassword);

    UserProfileVo getCurrentUser(String userId);
}

