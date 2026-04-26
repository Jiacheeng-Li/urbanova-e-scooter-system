package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_locations")
public class UserLocationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private BigDecimal lat;
    private BigDecimal lng;
    private String source;
    private LocalDateTime updatedAt;
}
