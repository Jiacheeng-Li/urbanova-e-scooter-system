package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_methods")
public class PaymentMethodEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentMethodId;
    private String userId;
    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String label;
    private Integer isDefault;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
