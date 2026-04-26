package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.entity.UserLocationEntity;
import com.lcyhz.urbanova.mapper.UserLocationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserLocationService {
    private final UserLocationMapper userLocationMapper;

    public UserLocationService(UserLocationMapper userLocationMapper) {
        this.userLocationMapper = userLocationMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertLocation(String userId, Map<String, Object> request) {
        BigDecimal lat = parseCoordinate(request == null ? null : request.get("lat"), "lat");
        BigDecimal lng = parseCoordinate(request == null ? null : request.get("lng"), "lng");
        String source = request != null && request.get("source") != null ? String.valueOf(request.get("source")) : "CLIENT_GPS";

        UserLocationEntity entity = userLocationMapper.selectOne(new LambdaQueryWrapper<UserLocationEntity>()
                .eq(UserLocationEntity::getUserId, userId));
        if (entity == null) {
            entity = new UserLocationEntity();
            entity.setUserId(userId);
            entity.setLat(lat);
            entity.setLng(lng);
            entity.setSource(source);
            entity.setUpdatedAt(LocalDateTime.now());
            userLocationMapper.insert(entity);
        } else {
            entity.setLat(lat);
            entity.setLng(lng);
            entity.setSource(source);
            entity.setUpdatedAt(LocalDateTime.now());
            userLocationMapper.updateById(entity);
        }
        return toMap(entity);
    }

    private BigDecimal parseCoordinate(Object value, String field) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        try {
            BigDecimal coordinate = new BigDecimal(String.valueOf(value));
            if ("lat".equals(field)
                    && (coordinate.compareTo(BigDecimal.valueOf(-90)) < 0 || coordinate.compareTo(BigDecimal.valueOf(90)) > 0)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "lat must be between -90 and 90");
            }
            if ("lng".equals(field)
                    && (coordinate.compareTo(BigDecimal.valueOf(-180)) < 0 || coordinate.compareTo(BigDecimal.valueOf(180)) > 0)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "lng must be between -180 and 180");
            }
            return coordinate;
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " must be numeric");
        }
    }

    private Map<String, Object> toMap(UserLocationEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", entity.getUserId());
        data.put("lat", entity.getLat());
        data.put("lng", entity.getLng());
        data.put("source", entity.getSource());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }
}
