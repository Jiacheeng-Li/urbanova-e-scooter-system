package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;

public interface AuthService {
    AuthPayload register(RegisterRequest request);

    AuthPayload login(LoginRequest request);

    UserProfileVo getCurrentUser(String userId);
}

