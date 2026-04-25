package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_sessions")
public class AuthSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String userId;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private Integer revoked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
