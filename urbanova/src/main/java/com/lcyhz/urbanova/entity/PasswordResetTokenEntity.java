package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("password_reset_tokens")
public class PasswordResetTokenEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String resetTokenId;
    private String userId;
    private String resetToken;
    private LocalDateTime expiresAt;
    private Integer used;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
