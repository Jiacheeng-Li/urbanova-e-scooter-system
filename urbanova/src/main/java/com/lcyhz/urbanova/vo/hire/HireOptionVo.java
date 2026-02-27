package com.lcyhz.urbanova.vo.hire;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HireOptionVo {
    private String hireOptionId;
    private String code;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private Boolean active;
}

