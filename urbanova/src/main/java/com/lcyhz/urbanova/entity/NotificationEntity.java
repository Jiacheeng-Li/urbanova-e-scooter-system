package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class NotificationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String message;
    private Integer readFlag;
    private String relatedBookingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
