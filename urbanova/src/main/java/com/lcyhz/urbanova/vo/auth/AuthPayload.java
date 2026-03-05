package com.lcyhz.urbanova.vo.auth;

import lombok.Data;

@Data
public class AuthPayload {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private UserProfileVo user;
}

