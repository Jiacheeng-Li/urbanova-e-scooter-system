package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

import java.util.List;

@Data
public class ScooterIdsByStatusVo {
    private String status;
    private List<String> scooterIds;
}

