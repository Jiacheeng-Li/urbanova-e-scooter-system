package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminScooterVo {
    private String scooterId;
    private String qrCodeId;
    private String typeCode;
    private String typeDisplayName;
    private String typeImageUrl;
    private String status;
    private Integer batteryPercent;
    private BigDecimal lat;
    private BigDecimal lng;
    private String zone;
    private Integer version;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
