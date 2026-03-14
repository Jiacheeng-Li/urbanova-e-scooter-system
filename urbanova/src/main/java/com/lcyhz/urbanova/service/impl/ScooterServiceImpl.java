package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        if (status == null || status.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "status is required");
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "invalid scooter status",
                    "Allowed values: AVAILABLE, RESERVED, IN_USE, MAINTENANCE, UNAVAILABLE");
        }

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
}

