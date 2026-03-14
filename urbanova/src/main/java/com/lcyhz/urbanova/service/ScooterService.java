package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;

public interface ScooterService {
    ScooterIdsByStatusVo queryScooterIdsByStatus(String status);
}

