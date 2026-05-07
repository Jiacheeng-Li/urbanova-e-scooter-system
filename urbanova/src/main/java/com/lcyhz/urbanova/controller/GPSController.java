package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.entity.GPSData;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.service.ScooterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lcyhz.urbanova.domain.ScooterPhoneID;

@RestController
@RequestMapping("/location")
public class GPSController {

    @Autowired
    private ScooterService scooterService1;

    @PostMapping
    public Map<String, Object> receiveData(@RequestBody GPSData data) {
        // 取出四个参数
        String deviceId = data.getDeviceId();
        double latitude = data.getLatitude();
        double longitude = data.getLongitude();
        int batteryLevel = data.getBatteryLevel();

        String scooterId = ScooterPhoneID.getScooterId(deviceId);
        UpdateScooterRequest request = new UpdateScooterRequest();
        request.setLat(BigDecimal.valueOf(latitude));
        request.setLng(BigDecimal.valueOf(longitude));
        request.setBatteryPercent(batteryLevel);
        scooterService1.updateScooter(scooterId, request);

        System.out.println("收到设备 " + deviceId + " 的数据：");
        System.out.println("  位置：(" + latitude + ", " + longitude + ")");
        System.out.println("  电量：" + batteryLevel + "%");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "数据接收成功");
        response.put("deviceId", deviceId);
        return response;
    }

    /**
     * 查询附近5km内的车辆
     * @param lat 中心点纬度
     * @param lng 中心点经度
     * @param radiusKm 半径（公里），默认5km
     * @return 附近车辆列表
     */
    @GetMapping("/nearby")
    public Map<String, Object> getNearbyScooters(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5") Double radiusKm) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 计算经纬度范围（粗略估算：1度 ≈ 111km）
            double delta = radiusKm / 111.0;
            double minLat = lat - delta;
            double maxLat = lat + delta;
            double minLng = lng - delta;
            double maxLng = lng + delta;

            // 查询范围内的车辆
            List<ScooterEntity> nearbyScooters = scooterService1.findNearbyScooters(
                    BigDecimal.valueOf(minLat),
                    BigDecimal.valueOf(maxLat),
                    BigDecimal.valueOf(minLng),
                    BigDecimal.valueOf(maxLng),
                    BigDecimal.valueOf(lat),
                    BigDecimal.valueOf(lng),
                    radiusKm
            );

            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", nearbyScooters);
            response.put("count", nearbyScooters.size());
            response.put("centerLat", lat);
            response.put("centerLng", lng);
            response.put("radiusKm", radiusKm);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
        }

        return response;
    }
}