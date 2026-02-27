package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class UserEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String role;
    private String discountCategory;
    private String accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

