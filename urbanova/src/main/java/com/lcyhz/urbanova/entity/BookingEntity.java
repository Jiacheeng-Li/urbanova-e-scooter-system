package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bookings")
public class BookingEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bookingId;
    private String bookingRef;
    private String customerType;
    private String userId;
    private String scooterId;
    private String hireOptionId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private BigDecimal priceBase;
    private BigDecimal priceDiscount;
    private BigDecimal priceFinal;
    private String paymentStatus;
    private String createdByRole;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

