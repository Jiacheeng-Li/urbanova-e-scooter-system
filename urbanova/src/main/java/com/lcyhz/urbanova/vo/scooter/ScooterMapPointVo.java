package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScooterMapPointVo {
    private String scooterId;
    private String status;
    private Integer batteryPercent;
    private BigDecimal lat;
    private BigDecimal lng;
    private String zone;
}
