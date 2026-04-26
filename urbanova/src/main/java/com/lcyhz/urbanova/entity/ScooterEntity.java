package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scooters")
public class ScooterEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scooterId;
    private String typeCode;
    private String status;
    private Integer batteryPercent;
    private BigDecimal lat;
    private BigDecimal lng;
    private String zone;
    private Integer version;
    private String color;
    private String qrCodeId;
    private LocalDateTime batteryUpdatedAt;
    private LocalDateTime chargeStartedAt;
    private LocalDateTime lowBatteryAlertedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
