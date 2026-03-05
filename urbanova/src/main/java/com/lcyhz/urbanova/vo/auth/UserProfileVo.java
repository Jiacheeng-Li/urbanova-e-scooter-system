package com.lcyhz.urbanova.vo.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVo {
    private String userId;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String discountCategory;
    private String accountStatus;
    private LocalDateTime createdAt;
}

