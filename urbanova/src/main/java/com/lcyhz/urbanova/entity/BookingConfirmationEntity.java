package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking_confirmations")
public class BookingConfirmationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String confirmationId;
    private String bookingId;
    private String userId;
    private String recipientEmail;
    private String channel;
    private String status;
    private String message;
    private Integer resendCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
