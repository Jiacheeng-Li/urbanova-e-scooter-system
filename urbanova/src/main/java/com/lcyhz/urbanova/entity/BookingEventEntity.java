package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking_events")
public class BookingEventEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String bookingId;
    private String eventType;
    private String actorUserId;
    private String actorRole;
    private String details;
    private LocalDateTime createdAt;
}
