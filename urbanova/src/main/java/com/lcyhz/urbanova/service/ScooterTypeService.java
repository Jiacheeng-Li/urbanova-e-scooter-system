package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.admin.scootertype.CreateScooterTypeRequest;
import com.lcyhz.urbanova.dto.admin.scootertype.UpdateScooterTypeRequest;
import com.lcyhz.urbanova.vo.scooter.AdminScooterTypeVo;
import com.lcyhz.urbanova.vo.scooter.ScooterTypeVo;

import java.util.List;

public interface ScooterTypeService {
    List<ScooterTypeVo> listActiveScooterTypes();

    ScooterTypeVo getScooterType(String typeCode);

    List<AdminScooterTypeVo> listAllScooterTypes();

    AdminScooterTypeVo createScooterType(CreateScooterTypeRequest request);

    AdminScooterTypeVo updateScooterType(String typeCode, UpdateScooterTypeRequest request);

    AdminScooterTypeVo disableScooterType(String typeCode);
}
