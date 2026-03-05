package com.lcyhz.urbanova.vo.booking;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceBreakdownVo {
    private BigDecimal base;
    private BigDecimal discount;
    private BigDecimal finalPrice;
}

