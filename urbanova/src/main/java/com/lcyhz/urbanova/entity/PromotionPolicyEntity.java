package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("promotion_policies")
public class PromotionPolicyEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String promotionPolicyId;
    private String policyCode;
    private String name;
    private String category;
    private String description;
    private BigDecimal percentage;
    private Integer minAge;
    private Integer maxAge;
    private Integer minCompletedBookings;
    private Integer maxCompletedBookings;
    private Integer holidayCampaign;
    private Integer stackable;
    private Integer priority;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
