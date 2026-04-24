package com.lcyhz.urbanova.vo.auth;

import lombok.Data;

@Data
public class AuthPayload {
    private String sessionId;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
    private long refreshExpiresInSeconds;
    private UserProfileVo user;
}

