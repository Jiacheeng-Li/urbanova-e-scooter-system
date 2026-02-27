package com.lcyhz.urbanova.vo.pricing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PriceQuoteVo {
    private BigDecimal basePrice;
    private List<AppliedDiscountVo> appliedDiscounts;
    private BigDecimal finalPrice;
    private String currency;
}

