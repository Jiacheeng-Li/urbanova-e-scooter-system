package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminScooterTypeVo {
    private String typeCode;
    private String displayName;
    private String imageUrl;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
