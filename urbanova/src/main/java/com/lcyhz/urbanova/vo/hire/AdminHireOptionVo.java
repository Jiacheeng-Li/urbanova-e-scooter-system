package com.lcyhz.urbanova.vo.hire;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminHireOptionVo {
    private String hireOptionId;
    private String code;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
