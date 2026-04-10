package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scooter_types")
public class ScooterTypeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeCode;
    private String displayName;
    private String imageUrl;
    private String description;
    private Integer active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
