package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.entity.ScooterTypeEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.entity.UserLocationEntity;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.mapper.ScooterTypeMapper;
import com.lcyhz.urbanova.mapper.UserLocationMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterVo;
import com.lcyhz.urbanova.vo.scooter.BulkScooterStatusUpdateVo;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import com.lcyhz.urbanova.vo.scooter.ScooterMapPointVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScooterServiceImpl implements ScooterService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            DomainConstants.ScooterStatus.AVAILABLE,
            DomainConstants.ScooterStatus.RESERVED,
            DomainConstants.ScooterStatus.IN_USE,
            DomainConstants.ScooterStatus.MAINTENANCE,
            DomainConstants.ScooterStatus.UNAVAILABLE,
            DomainConstants.ScooterStatus.FAULT,
            DomainConstants.ScooterStatus.UNDER_REPAIR,
            DomainConstants.ScooterStatus.LOW_BATTERY,
            DomainConstants.ScooterStatus.CHARGING
    );

    private final ScooterMapper scooterMapper;
    private final ScooterTypeMapper scooterTypeMapper;
    private final UserMapper userMapper;
    private final UserLocationMapper userLocationMapper;
    private final PlatformSupportService platformSupportService;

    @Value("${app.scooter.low-battery-threshold:20}")
    private int lowBatteryThreshold;

    @Value("${app.scooter.battery-drain-per-minute:1}")
    private int batteryDrainPerMinute;

    @Value("${app.scooter.charge-duration-minutes:3}")
    private int chargeDurationMinutes;

    @Override
    public List<Map<String, Object>> listPublicScooters(String status, String typeCode, String zone) {
        LambdaQueryWrapper<ScooterEntity> query = new LambdaQueryWrapper<ScooterEntity>()
                .orderByAsc(ScooterEntity::getScooterId);
        if (status != null && !status.isBlank()) {
            query.eq(ScooterEntity::getStatus, normalizeScooterStatus(status));
        }
        if (typeCode != null && !typeCode.isBlank()) {
            query.eq(ScooterEntity::getTypeCode, normalizeTypeCode(typeCode));
        }
        if (zone != null && !zone.isBlank()) {
            query.eq(ScooterEntity::getZone, zone.trim());
        }
        Map<String, ScooterTypeEntity> typeMap = loadScooterTypeMap();
        return scooterMapper.selectList(query).stream()
                .map(entity -> toPublicScooterMap(entity, typeMap.get(entity.getTypeCode())))
                .toList();
    }

    @Override
    public Map<String, Object> getScooterDetail(String scooterId) {
        ScooterEntity entity = findScooter(scooterId);
        return toPublicScooterMap(entity, loadScooterTypeMap().get(entity.getTypeCode()));
    }

    @Override
    public Map<String, Object> getMapView(String userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scooters", listMapPoints());
        UserLocationEntity location = userLocationMapper.selectOne(new LambdaQueryWrapper<UserLocationEntity>()
                .eq(UserLocationEntity::getUserId, userId));
        data.put("userLocation", location == null ? null : toUserLocationMap(location));
        return data;
    }

    @Override
    public Map<String, Object> getAvailabilitySummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String status : ALLOWED_STATUSES) {
            long count = scooterMapper.selectCount(new LambdaQueryWrapper<ScooterEntity>()
                    .eq(ScooterEntity::getStatus, status));
            data.put(status, count);
        }
        data.put("total", scooterMapper.selectCount(new LambdaQueryWrapper<>()));
        return data;
    }

    @Override
    public Map<String, Object> getScooterQrMetadata(String scooterId) {
        ScooterEntity entity = findScooter(scooterId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scooterId", entity.getScooterId());
        data.put("qrCodeId", entity.getQrCodeId());
        data.put("payload", qrPayload(entity.getQrCodeId()));
        data.put("imagePath", "/api/v1/scooters/qr/" + entity.getQrCodeId() + "/image");
        data.put("resolvePath", "/api/v1/scooters/qr/" + entity.getQrCodeId());
        return data;
    }

    @Override
    public Map<String, Object> getScooterByQrCodeId(String qrCodeId) {
        ScooterEntity entity = findByQrCodeId(qrCodeId);
        return toPublicScooterMap(entity, loadScooterTypeMap().get(entity.getTypeCode()));
    }

    @Override
    public byte[] renderScooterQrCode(String qrCodeId) {
        ScooterEntity entity = findByQrCodeId(qrCodeId);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(qrPayload(entity.getQrCodeId()), BarcodeFormat.QR_CODE, 360, 360);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCodes.INTERNAL_ERROR, "Failed to generate QR code");
        }
    }

    @Override
    public ScooterIdsByStatusVo queryScooterIdsByStatus(String status) {
        String normalizedStatus = normalizeScooterStatus(status);
        List<String> scooterIds = scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                        .eq(ScooterEntity::getStatus, normalizedStatus)
                        .orderByAsc(ScooterEntity::getScooterId))
                .stream()
                .map(ScooterEntity::getScooterId)
                .toList();
        ScooterIdsByStatusVo response = new ScooterIdsByStatusVo();
        response.setStatus(normalizedStatus);
        response.setScooterIds(scooterIds);
        return response;
    }

    @Override
    public List<ScooterMapPointVo> listMapPoints() {
        List<ScooterEntity> scooters = scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                .isNotNull(ScooterEntity::getLat)
                .isNotNull(ScooterEntity::getLng)
                .orderByAsc(ScooterEntity::getScooterId));
        Map<String, ScooterTypeEntity> typeMap = loadScooterTypeMap();
        return scooters.stream().map(scooter -> toMapPointVo(scooter, typeMap.get(scooter.getTypeCode()))).toList();
    }

    @Override
    public List<AdminScooterVo> listAdminScooters(String status) {
        LambdaQueryWrapper<ScooterEntity> query = new LambdaQueryWrapper<ScooterEntity>()
                .orderByAsc(ScooterEntity::getScooterId);
        if (status != null && !status.isBlank()) {
            query.eq(ScooterEntity::getStatus, normalizeScooterStatus(status));
        }
        List<ScooterEntity> scooters = scooterMapper.selectList(query);
        Map<String, ScooterTypeEntity> typeMap = loadScooterTypeMap();
        return scooters.stream().map(scooter -> toAdminVo(scooter, typeMap.get(scooter.getTypeCode()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterVo createScooter(CreateScooterRequest request) {
        String scooterId = normalizeScooterId(request.getScooterId());
        ScooterEntity existing = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, scooterId));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Scooter already exists");
        }

        BigDecimal lat = request.getLat();
        BigDecimal lng = request.getLng();
        validateCoordinates(lat, lng);
        LocalDateTime now = LocalDateTime.now();

        ScooterEntity entity = new ScooterEntity();
        entity.setScooterId(scooterId);
        entity.setColor(request.getColor());
        entity.setTypeCode(requireScooterType(request.getTypeCode()).getTypeCode());
        entity.setBatteryPercent(request.getBatteryPercent() == null ? 100 : request.getBatteryPercent());
        entity.setLat(lat);
        entity.setLng(lng);
        entity.setZone(trimToNull(request.getZone()));
        entity.setVersion(0);
        entity.setQrCodeId(newQrCodeId());
        entity.setBatteryUpdatedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            entity.setStatus(entity.getBatteryPercent() < lowBatteryThreshold
                    ? DomainConstants.ScooterStatus.LOW_BATTERY
                    : DomainConstants.ScooterStatus.AVAILABLE);
        } else {
            entity.setStatus(normalizeScooterStatus(request.getStatus()));
        }
        entity.setChargeStartedAt(DomainConstants.ScooterStatus.CHARGING.equals(entity.getStatus()) ? now : null);
        scooterMapper.insert(entity);
        return toAdminVo(entity, requireScooterType(entity.getTypeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterVo updateScooter(String scooterId, UpdateScooterRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: typeCode, batteryPercent, lat, lng, zone, color");
        }

        ScooterEntity entity = findScooter(scooterId);
        BigDecimal effectiveLat = request.getLat() != null ? request.getLat() : entity.getLat();
        BigDecimal effectiveLng = request.getLng() != null ? request.getLng() : entity.getLng();
        validateCoordinates(effectiveLat, effectiveLng);

        if (request.getColor() != null) {
            entity.setColor(request.getColor());
        }
        if (request.getBatteryPercent() != null) {
            entity.setBatteryPercent(request.getBatteryPercent());
            entity.setBatteryUpdatedAt(LocalDateTime.now());
            if (request.getBatteryPercent() >= lowBatteryThreshold) {
                entity.setLowBatteryAlertedAt(null);
            }
        }
        if (request.getTypeCode() != null) {
            entity.setTypeCode(requireScooterType(request.getTypeCode()).getTypeCode());
        }
        if (request.getLat() != null) {
            entity.setLat(request.getLat());
        }
        if (request.getLng() != null) {
            entity.setLng(request.getLng());
        }
        if (request.getZone() != null) {
            entity.setZone(trimToNull(request.getZone()));
        }

        syncIdleStatusByBattery(entity);
        entity.setVersion(nextVersion(entity.getVersion()));
        entity.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(entity);
        return toAdminVo(entity, requireScooterType(entity.getTypeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterVo updateScooterStatus(String scooterId, UpdateScooterStatusRequest request) {
        ScooterEntity entity = findScooter(scooterId);
        String normalizedStatus = normalizeScooterStatus(request.getStatus());
        if (DomainConstants.ScooterStatus.CHARGING.equals(normalizedStatus)) {
            entity.setChargeStartedAt(LocalDateTime.now());
            entity.setBatteryUpdatedAt(LocalDateTime.now());
        } else if (!DomainConstants.ScooterStatus.IN_USE.equals(normalizedStatus)) {
            entity.setChargeStartedAt(null);
        }
        entity.setStatus(normalizedStatus);
        syncIdleStatusByBattery(entity);
        entity.setVersion(nextVersion(entity.getVersion()));
        entity.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(entity);
        return toAdminVo(entity, requireScooterType(entity.getTypeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BulkScooterStatusUpdateVo bulkUpdateScooterStatus(BulkUpdateScooterStatusRequest request) {
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        for (String scooterId : request.getScooterIds()) {
            normalizedIds.add(normalizeScooterId(scooterId));
        }
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "scooterIds is required");
        }

        String normalizedStatus = normalizeScooterStatus(request.getStatus());
        List<ScooterEntity> scooters = scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                .in(ScooterEntity::getScooterId, normalizedIds));
        if (scooters.size() != normalizedIds.size()) {
            Set<String> foundIds = scooters.stream().map(ScooterEntity::getScooterId).collect(Collectors.toSet());
            List<String> missingIds = normalizedIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND,
                    "Some scooters were not found", missingIds);
        }

        LocalDateTime now = LocalDateTime.now();
        for (ScooterEntity scooter : scooters) {
            scooter.setStatus(normalizedStatus);
            scooter.setChargeStartedAt(DomainConstants.ScooterStatus.CHARGING.equals(normalizedStatus) ? now : null);
            if (DomainConstants.ScooterStatus.CHARGING.equals(normalizedStatus)) {
                scooter.setBatteryUpdatedAt(now);
            }
            syncIdleStatusByBattery(scooter);
            scooter.setVersion(nextVersion(scooter.getVersion()));
            scooter.setUpdatedAt(now);
            scooterMapper.updateById(scooter);
        }

        BulkScooterStatusUpdateVo response = new BulkScooterStatusUpdateVo();
        response.setStatus(normalizedStatus);
        response.setUpdatedCount(scooters.size());
        response.setScooterIds(new ArrayList<>(normalizedIds));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startCharging(String scooterId) {
        ScooterEntity entity = findScooter(scooterId);
        if (!Set.of(
                DomainConstants.ScooterStatus.AVAILABLE,
                DomainConstants.ScooterStatus.LOW_BATTERY,
                DomainConstants.ScooterStatus.MAINTENANCE,
                DomainConstants.ScooterStatus.UNAVAILABLE
        ).contains(entity.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_CHARGING_NOT_ALLOWED,
                    "Charging cannot be started in the current scooter state");
        }
        entity.setStatus(DomainConstants.ScooterStatus.CHARGING);
        entity.setChargeStartedAt(LocalDateTime.now());
        entity.setBatteryUpdatedAt(LocalDateTime.now());
        entity.setVersion(nextVersion(entity.getVersion()));
        entity.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(entity);
        return toPublicScooterMap(entity, requireScooterType(entity.getTypeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processScooterLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        processInUseDrain(now);
        processChargingCompletion(now);
    }

    private void processInUseDrain(LocalDateTime now) {
        List<ScooterEntity> scooters = scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getStatus, DomainConstants.ScooterStatus.IN_USE));
        for (ScooterEntity scooter : scooters) {
            LocalDateTime lastTick = scooter.getBatteryUpdatedAt() == null ? now : scooter.getBatteryUpdatedAt();
            long elapsedMinutes = Duration.between(lastTick, now).toMinutes();
            if (elapsedMinutes <= 0) {
                continue;
            }
            int oldBattery = scooter.getBatteryPercent() == null ? 100 : scooter.getBatteryPercent();
            int drain = Math.toIntExact(elapsedMinutes * Math.max(1, batteryDrainPerMinute));
            int newBattery = Math.max(0, oldBattery - drain);
            scooter.setBatteryPercent(newBattery);
            scooter.setBatteryUpdatedAt(lastTick.plusMinutes(elapsedMinutes));
            scooter.setUpdatedAt(now);
            scooter.setVersion(nextVersion(scooter.getVersion()));
            if (oldBattery >= lowBatteryThreshold && newBattery < lowBatteryThreshold) {
                scooter.setLowBatteryAlertedAt(now);
                notifyManagersLowBattery(scooter);
            }
            scooterMapper.updateById(scooter);
        }
    }

    private void processChargingCompletion(LocalDateTime now) {
        List<ScooterEntity> chargingScooters = scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getStatus, DomainConstants.ScooterStatus.CHARGING));
        for (ScooterEntity scooter : chargingScooters) {
            if (scooter.getChargeStartedAt() == null) {
                scooter.setChargeStartedAt(now);
                scooterMapper.updateById(scooter);
                continue;
            }
            long chargingMinutes = Duration.between(scooter.getChargeStartedAt(), now).toMinutes();
            if (chargingMinutes < chargeDurationMinutes) {
                continue;
            }
            scooter.setBatteryPercent(100);
            scooter.setStatus(DomainConstants.ScooterStatus.AVAILABLE);
            scooter.setChargeStartedAt(null);
            scooter.setLowBatteryAlertedAt(null);
            scooter.setBatteryUpdatedAt(now);
            scooter.setUpdatedAt(now);
            scooter.setVersion(nextVersion(scooter.getVersion()));
            scooterMapper.updateById(scooter);
            notifyManagersChargingCompleted(scooter);
        }
    }

    private void notifyManagersLowBattery(ScooterEntity scooter) {
        String message = "Scooter " + scooter.getScooterId() + " dropped below " + lowBatteryThreshold + "% battery and should be charged.";
        for (UserEntity manager : activeManagers()) {
            platformSupportService.createNotification(manager.getUserId(), DomainConstants.NotificationType.SCOOTER_LOW_BATTERY,
                    "Low battery alert", message, null);
        }
        platformSupportService.recordAudit(null, DomainConstants.ROLE_SYSTEM, "SCOOTER_LOW_BATTERY", "SCOOTER", scooter.getScooterId(),
                "batteryPercent=" + scooter.getBatteryPercent());
    }

    private void notifyManagersChargingCompleted(ScooterEntity scooter) {
        String message = "Scooter " + scooter.getScooterId() + " finished charging and is now at 100% battery.";
        for (UserEntity manager : activeManagers()) {
            platformSupportService.createNotification(manager.getUserId(), DomainConstants.NotificationType.SCOOTER_CHARGED,
                    "Charging completed", message, null);
        }
        platformSupportService.recordAudit(null, DomainConstants.ROLE_SYSTEM, "SCOOTER_CHARGED", "SCOOTER", scooter.getScooterId(),
                "batteryPercent=100");
    }

    private List<UserEntity> activeManagers() {
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRole, DomainConstants.ROLE_MANAGER)
                .eq(UserEntity::getAccountStatus, DomainConstants.ACCOUNT_ACTIVE));
    }

    private ScooterEntity findScooter(String scooterId) {
        ScooterEntity entity = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, normalizeScooterId(scooterId)));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter not found");
        }
        return entity;
    }

    private ScooterEntity findByQrCodeId(String qrCodeId) {
        String normalizedQrCodeId = normalizeQrCodeId(qrCodeId);
        ScooterEntity entity = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getQrCodeId, normalizedQrCodeId));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.QR_CODE_NOT_FOUND, "Scooter QR code not found");
        }
        return entity;
    }

    private ScooterTypeEntity requireScooterType(String typeCode) {
        ScooterTypeEntity entity = scooterTypeMapper.selectOne(new LambdaQueryWrapper<ScooterTypeEntity>()
                .eq(ScooterTypeEntity::getTypeCode, normalizeTypeCode(typeCode))
                .eq(ScooterTypeEntity::getActive, 1));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter type not found");
        }
        return entity;
    }

    private void syncIdleStatusByBattery(ScooterEntity entity) {
        if (entity.getBatteryPercent() == null) {
            return;
        }
        if (Set.of(DomainConstants.ScooterStatus.AVAILABLE, DomainConstants.ScooterStatus.LOW_BATTERY).contains(entity.getStatus())) {
            entity.setStatus(entity.getBatteryPercent() < lowBatteryThreshold
                    ? DomainConstants.ScooterStatus.LOW_BATTERY
                    : DomainConstants.ScooterStatus.AVAILABLE);
        }
    }

    private String normalizeScooterId(String scooterId) {
        if (scooterId == null || scooterId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "scooterId is required");
        }
        return scooterId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeQrCodeId(String qrCodeId) {
        if (qrCodeId == null || qrCodeId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "qrCodeId is required");
        }
        return qrCodeId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTypeCode(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "typeCode is required");
        }
        return typeCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeScooterStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "status is required");
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "invalid scooter status",
                    "Allowed values: AVAILABLE, RESERVED, IN_USE, MAINTENANCE, UNAVAILABLE, FAULT, UNDER_REPAIR, LOW_BATTERY, CHARGING");
        }
        return normalizedStatus;
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if ((lat == null) != (lng == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "lat and lng must be provided together");
        }
        if (lat != null && (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "lat must be between -90 and 90");
        }
        if (lng != null && (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "lng must be between -180 and 180");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer nextVersion(Integer version) {
        return version == null ? 1 : version + 1;
    }

    private String newQrCodeId() {
        return "QR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String qrPayload(String qrCodeId) {
        return "URBANOVA:SCOOTER:QR:" + qrCodeId;
    }

    private Map<String, ScooterTypeEntity> loadScooterTypeMap() {
        return scooterTypeMapper.selectList(new LambdaQueryWrapper<ScooterTypeEntity>())
                .stream()
                .collect(Collectors.toMap(ScooterTypeEntity::getTypeCode, Function.identity()));
    }

    private ScooterMapPointVo toMapPointVo(ScooterEntity entity, ScooterTypeEntity typeEntity) {
        ScooterMapPointVo vo = new ScooterMapPointVo();
        vo.setScooterId(entity.getScooterId());
        vo.setQrCodeId(entity.getQrCodeId());
        vo.setTypeCode(entity.getTypeCode());
        if (typeEntity != null) {
            vo.setTypeDisplayName(typeEntity.getDisplayName());
            vo.setTypeImageUrl(typeEntity.getImageUrl());
        }
        vo.setStatus(entity.getStatus());
        vo.setBatteryPercent(entity.getBatteryPercent());
        vo.setLat(entity.getLat());
        vo.setLng(entity.getLng());
        vo.setZone(entity.getZone());
        return vo;
    }

    private AdminScooterVo toAdminVo(ScooterEntity entity, ScooterTypeEntity typeEntity) {
        AdminScooterVo vo = new AdminScooterVo();
        vo.setScooterId(entity.getScooterId());
        vo.setQrCodeId(entity.getQrCodeId());
        vo.setTypeCode(entity.getTypeCode());
        if (typeEntity != null) {
            vo.setTypeDisplayName(typeEntity.getDisplayName());
            vo.setTypeImageUrl(typeEntity.getImageUrl());
        }
        vo.setColor(entity.getColor());
        vo.setStatus(entity.getStatus());
        vo.setBatteryPercent(entity.getBatteryPercent());
        vo.setLat(entity.getLat());
        vo.setLng(entity.getLng());
        vo.setZone(entity.getZone());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toPublicScooterMap(ScooterEntity entity, ScooterTypeEntity typeEntity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scooterId", entity.getScooterId());
        data.put("qrCodeId", entity.getQrCodeId());
        data.put("typeCode", entity.getTypeCode());
        if (typeEntity != null) {
            data.put("typeDisplayName", typeEntity.getDisplayName());
            data.put("typeImageUrl", typeEntity.getImageUrl());
            data.put("typeDescription", typeEntity.getDescription());
        }
        data.put("status", entity.getStatus());
        data.put("batteryPercent", entity.getBatteryPercent());
        data.put("lat", entity.getLat());
        data.put("lng", entity.getLng());
        data.put("zone", entity.getZone());
        data.put("version", entity.getVersion());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private Map<String, Object> toUserLocationMap(UserLocationEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", entity.getUserId());
        data.put("lat", entity.getLat());
        data.put("lng", entity.getLng());
        data.put("source", entity.getSource());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }
}
