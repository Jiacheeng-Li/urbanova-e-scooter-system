package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.vo.scooter.AdminScooterVo;
import com.lcyhz.urbanova.vo.scooter.BulkScooterStatusUpdateVo;
import com.lcyhz.urbanova.vo.scooter.ScooterMapPointVo;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;

import java.util.Map;
import java.util.List;

public interface ScooterService {
    List<Map<String, Object>> listPublicScooters(String status, String typeCode, String zone);

    Map<String, Object> getScooterDetail(String scooterId);

    Map<String, Object> getAvailabilitySummary();

    ScooterIdsByStatusVo queryScooterIdsByStatus(String status);

    List<ScooterMapPointVo> listMapPoints();

    List<AdminScooterVo> listAdminScooters(String status);

    AdminScooterVo createScooter(CreateScooterRequest request);

    AdminScooterVo updateScooter(String scooterId, UpdateScooterRequest request);

    AdminScooterVo updateScooterStatus(String scooterId, UpdateScooterStatusRequest request);

    BulkScooterStatusUpdateVo bulkUpdateScooterStatus(BulkUpdateScooterStatusRequest request);
}
