package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hire_options")
public class HireOptionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String hireOptionId;
    private String code;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private Integer active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

