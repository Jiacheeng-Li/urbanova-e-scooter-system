package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_verification_codes")
public class EmailVerificationCodeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String verificationCodeId;
    private String email;
    private String purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private Integer consumed;
    private Integer attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
