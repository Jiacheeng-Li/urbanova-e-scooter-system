package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterVo;
import com.lcyhz.urbanova.vo.scooter.BulkScooterStatusUpdateVo;
import com.lcyhz.urbanova.vo.scooter.ScooterMapPointVo;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScooterServiceImpl implements ScooterService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            DomainConstants.ScooterStatus.AVAILABLE,
            DomainConstants.ScooterStatus.RESERVED,
            DomainConstants.ScooterStatus.IN_USE,
            DomainConstants.ScooterStatus.MAINTENANCE,
            DomainConstants.ScooterStatus.UNAVAILABLE
    );

    private final ScooterMapper scooterMapper;

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
        return scooterMapper.selectList(new LambdaQueryWrapper<ScooterEntity>()
                        .isNotNull(ScooterEntity::getLat)
                        .isNotNull(ScooterEntity::getLng)
                        .orderByAsc(ScooterEntity::getScooterId))
                .stream()
                .map(this::toMapPointVo)
                .toList();
    }

    @Override
    public List<AdminScooterVo> listAdminScooters(String status) {
        LambdaQueryWrapper<ScooterEntity> query = new LambdaQueryWrapper<ScooterEntity>()
                .orderByAsc(ScooterEntity::getScooterId);
        if (status != null && !status.isBlank()) {
            query.eq(ScooterEntity::getStatus, normalizeScooterStatus(status));
        }
        return scooterMapper.selectList(query).stream().map(this::toAdminVo).toList();
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
        entity.setStatus(request.getStatus() == null || request.getStatus().isBlank()
                ? DomainConstants.ScooterStatus.AVAILABLE
                : normalizeScooterStatus(request.getStatus()));
        entity.setBatteryPercent(request.getBatteryPercent() == null ? 100 : request.getBatteryPercent());
        entity.setLat(lat);
        entity.setLng(lng);
        entity.setZone(trimToNull(request.getZone()));
        entity.setVersion(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        scooterMapper.insert(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterVo updateScooter(String scooterId, UpdateScooterRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: batteryPercent, lat, lng, zone");
        }

        ScooterEntity entity = findScooter(scooterId);
        BigDecimal effectiveLat = request.getLat() != null ? request.getLat() : entity.getLat();
        BigDecimal effectiveLng = request.getLng() != null ? request.getLng() : entity.getLng();
        validateCoordinates(effectiveLat, effectiveLng);

        if (request.getBatteryPercent() != null) {
            entity.setBatteryPercent(request.getBatteryPercent());
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
        entity.setVersion(nextVersion(entity.getVersion()));
        entity.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterVo updateScooterStatus(String scooterId, UpdateScooterStatusRequest request) {
        ScooterEntity entity = findScooter(scooterId);
        entity.setStatus(normalizeScooterStatus(request.getStatus()));
        entity.setVersion(nextVersion(entity.getVersion()));
        entity.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(entity);
        return toAdminVo(entity);
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
            Set<String> foundIds = scooters.stream().map(ScooterEntity::getScooterId).collect(java.util.stream.Collectors.toSet());
            List<String> missingIds = normalizedIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND,
                    "Some scooters were not found", missingIds);
        }

        LocalDateTime now = LocalDateTime.now();
        for (ScooterEntity scooter : scooters) {
            scooter.setStatus(normalizedStatus);
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

    private ScooterEntity findScooter(String scooterId) {
        ScooterEntity entity = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, normalizeScooterId(scooterId)));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter not found");
        }
        return entity;
    }

    private String normalizeScooterId(String scooterId) {
        if (scooterId == null || scooterId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "scooterId is required");
        }
        return scooterId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeScooterStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "status is required");
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "invalid scooter status",
                    "Allowed values: AVAILABLE, RESERVED, IN_USE, MAINTENANCE, UNAVAILABLE");
        }
        return normalizedStatus;
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if ((lat == null) != (lng == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "lat and lng must be provided together");
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

    private ScooterMapPointVo toMapPointVo(ScooterEntity entity) {
        ScooterMapPointVo vo = new ScooterMapPointVo();
        vo.setScooterId(entity.getScooterId());
        vo.setStatus(entity.getStatus());
        vo.setBatteryPercent(entity.getBatteryPercent());
        vo.setLat(entity.getLat());
        vo.setLng(entity.getLng());
        vo.setZone(entity.getZone());
        return vo;
    }

    private AdminScooterVo toAdminVo(ScooterEntity entity) {
        AdminScooterVo vo = new AdminScooterVo();
        vo.setScooterId(entity.getScooterId());
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
}
