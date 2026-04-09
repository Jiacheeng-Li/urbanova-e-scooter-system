package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

@Data
public class ScooterTypeVo {
    private String typeCode;
    private String displayName;
    private String imageUrl;
    private String description;
    private Boolean active;
}
