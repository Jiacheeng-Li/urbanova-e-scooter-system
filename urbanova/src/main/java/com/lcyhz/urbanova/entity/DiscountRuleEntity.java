package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("discount_rules")
public class DiscountRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String discountRuleId;
    private String type;
    private BigDecimal thresholdHoursPerWeek;
    private BigDecimal percentage;
    private Integer active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
