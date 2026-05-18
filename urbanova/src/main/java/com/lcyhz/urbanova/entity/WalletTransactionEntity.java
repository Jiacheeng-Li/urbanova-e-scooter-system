package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_transactions")
public class WalletTransactionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String walletTransactionId;
    private String walletAccountId;
    private String userId;
    private String type;
    private BigDecimal amount;
    private String method;
    private String paymentMethodId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
