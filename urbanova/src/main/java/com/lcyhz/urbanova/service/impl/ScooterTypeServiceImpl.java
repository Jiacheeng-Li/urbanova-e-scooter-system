package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.dto.admin.scootertype.CreateScooterTypeRequest;
import com.lcyhz.urbanova.dto.admin.scootertype.UpdateScooterTypeRequest;
import com.lcyhz.urbanova.entity.ScooterTypeEntity;
import com.lcyhz.urbanova.mapper.ScooterTypeMapper;
import com.lcyhz.urbanova.service.ScooterTypeService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterTypeVo;
import com.lcyhz.urbanova.vo.scooter.ScooterTypeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ScooterTypeServiceImpl implements ScooterTypeService {
    private final ScooterTypeMapper scooterTypeMapper;

    @Override
    public List<ScooterTypeVo> listActiveScooterTypes() {
        return scooterTypeMapper.selectList(new LambdaQueryWrapper<ScooterTypeEntity>()
                        .eq(ScooterTypeEntity::getActive, 1)
                        .orderByAsc(ScooterTypeEntity::getDisplayName))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public ScooterTypeVo getScooterType(String typeCode) {
        String normalizedTypeCode = normalizeTypeCode(typeCode);
        ScooterTypeEntity entity = scooterTypeMapper.selectOne(new LambdaQueryWrapper<ScooterTypeEntity>()
                .eq(ScooterTypeEntity::getTypeCode, normalizedTypeCode)
                .eq(ScooterTypeEntity::getActive, 1));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter type not found");
        }
        return toVo(entity);
    }

    @Override
    public List<AdminScooterTypeVo> listAllScooterTypes() {
        return scooterTypeMapper.selectList(new LambdaQueryWrapper<ScooterTypeEntity>()
                        .orderByAsc(ScooterTypeEntity::getDisplayName))
                .stream()
                .map(this::toAdminVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterTypeVo createScooterType(CreateScooterTypeRequest request) {
        String normalizedTypeCode = normalizeTypeCode(request.getTypeCode());
        ScooterTypeEntity existing = scooterTypeMapper.selectOne(new LambdaQueryWrapper<ScooterTypeEntity>()
                .eq(ScooterTypeEntity::getTypeCode, normalizedTypeCode));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Scooter type already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        ScooterTypeEntity entity = new ScooterTypeEntity();
        entity.setTypeCode(normalizedTypeCode);
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setImageUrl(request.getImageUrl().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setActive(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        scooterTypeMapper.insert(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterTypeVo updateScooterType(String typeCode, UpdateScooterTypeRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: displayName, imageUrl, description");
        }

        ScooterTypeEntity entity = findScooterType(typeCode);
        if (request.getDisplayName() != null) {
            entity.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(trimToNull(request.getDescription()));
        }
        entity.setUpdatedAt(LocalDateTime.now());
        scooterTypeMapper.updateById(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScooterTypeVo disableScooterType(String typeCode) {
        ScooterTypeEntity entity = findScooterType(typeCode);
        entity.setActive(0);
        entity.setUpdatedAt(LocalDateTime.now());
        scooterTypeMapper.updateById(entity);
        return toAdminVo(entity);
    }

    private String normalizeTypeCode(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "typeCode is required");
        }
        return typeCode.trim().toUpperCase(Locale.ROOT);
    }

    private ScooterTypeEntity findScooterType(String typeCode) {
        ScooterTypeEntity entity = scooterTypeMapper.selectOne(new LambdaQueryWrapper<ScooterTypeEntity>()
                .eq(ScooterTypeEntity::getTypeCode, normalizeTypeCode(typeCode)));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter type not found");
        }
        return entity;
    }

    private ScooterTypeVo toVo(ScooterTypeEntity entity) {
        ScooterTypeVo vo = new ScooterTypeVo();
        vo.setTypeCode(entity.getTypeCode());
        vo.setDisplayName(entity.getDisplayName());
        vo.setImageUrl(entity.getImageUrl());
        vo.setDescription(entity.getDescription());
        vo.setActive(entity.getActive() != null && entity.getActive() == 1);
        return vo;
    }

    private AdminScooterTypeVo toAdminVo(ScooterTypeEntity entity) {
        AdminScooterTypeVo vo = new AdminScooterTypeVo();
        vo.setTypeCode(entity.getTypeCode());
        vo.setDisplayName(entity.getDisplayName());
        vo.setImageUrl(entity.getImageUrl());
        vo.setDescription(entity.getDescription());
        vo.setActive(entity.getActive() != null && entity.getActive() == 1);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
