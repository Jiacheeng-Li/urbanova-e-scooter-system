package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("idempotency_keys")
public class IdempotencyKeyEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String idempotencyKey;
    private String scope;
    private String requestHash;
    private Integer responseCode;
    private String responseRef;
    private LocalDateTime createdAt;
}
