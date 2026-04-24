package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payments")
public class PaymentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentId;
    private String bookingId;
    private String userId;
    private BigDecimal amount;
    private String method;
    private String paymentMethodId;
    private String status;
    private String simulatedOutcome;
    private BigDecimal refundedAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
