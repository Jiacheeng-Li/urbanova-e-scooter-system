package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.dto.admin.hire.CreateHireOptionRequest;
import com.lcyhz.urbanova.dto.admin.hire.UpdateHireOptionRequest;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.service.HireOptionService;
import com.lcyhz.urbanova.vo.hire.AdminHireOptionVo;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HireOptionServiceImpl implements HireOptionService {
    private final HireOptionMapper hireOptionMapper;

    @Override
    public List<HireOptionVo> listActiveHireOptions() {
        List<HireOptionEntity> options = hireOptionMapper.selectList(new LambdaQueryWrapper<HireOptionEntity>()
                .eq(HireOptionEntity::getActive, 1)
                .orderByAsc(HireOptionEntity::getDurationMinutes));
        return options.stream().map(this::toVo).toList();
    }

    @Override
    public PriceQuoteVo quotePrice(PriceQuoteRequest request) {
        HireOptionEntity option = hireOptionMapper.selectOne(new LambdaQueryWrapper<HireOptionEntity>()
                .eq(HireOptionEntity::getCode, request.getHireOptionCode())
                .eq(HireOptionEntity::getActive, 1));
        if (option == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Hire option not found");
        }

        PriceQuoteVo quoteVo = new PriceQuoteVo();
        quoteVo.setBasePrice(option.getBasePrice());
        quoteVo.setAppliedDiscounts(Collections.emptyList());
        quoteVo.setFinalPrice(option.getBasePrice().subtract(BigDecimal.ZERO));
        quoteVo.setCurrency(DomainConstants.CURRENCY_GBP);
        return quoteVo;
    }

    @Override
    public List<AdminHireOptionVo> listAllHireOptions() {
        return hireOptionMapper.selectList(new LambdaQueryWrapper<HireOptionEntity>()
                        .orderByAsc(HireOptionEntity::getDurationMinutes)
                        .orderByAsc(HireOptionEntity::getCode))
                .stream()
                .map(this::toAdminVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminHireOptionVo createHireOption(CreateHireOptionRequest request) {
        String normalizedCode = normalizeCode(request.getCode());
        String hireOptionId = "HIRE-" + normalizedCode;

        HireOptionEntity existing = hireOptionMapper.selectOne(new LambdaQueryWrapper<HireOptionEntity>()
                .and(wrapper -> wrapper.eq(HireOptionEntity::getCode, normalizedCode)
                        .or()
                        .eq(HireOptionEntity::getHireOptionId, hireOptionId)));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "Hire option already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        HireOptionEntity entity = new HireOptionEntity();
        entity.setHireOptionId(hireOptionId);
        entity.setCode(normalizedCode);
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setBasePrice(request.getBasePrice());
        entity.setActive(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        hireOptionMapper.insert(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminHireOptionVo updateHireOption(String hireOptionId, UpdateHireOptionRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: durationMinutes, basePrice");
        }

        HireOptionEntity entity = findHireOptionForAdmin(hireOptionId);
        if (request.getDurationMinutes() != null) {
            entity.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getBasePrice() != null) {
            entity.setBasePrice(request.getBasePrice());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        hireOptionMapper.updateById(entity);
        return toAdminVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminHireOptionVo disableHireOption(String hireOptionId) {
        HireOptionEntity entity = findHireOptionForAdmin(hireOptionId);
        entity.setActive(0);
        entity.setUpdatedAt(LocalDateTime.now());
        hireOptionMapper.updateById(entity);
        return toAdminVo(entity);
    }

    private HireOptionVo toVo(HireOptionEntity entity) {
        HireOptionVo vo = new HireOptionVo();
        vo.setHireOptionId(entity.getHireOptionId());
        vo.setCode(entity.getCode());
        vo.setDurationMinutes(entity.getDurationMinutes());
        vo.setBasePrice(entity.getBasePrice());
        vo.setActive(entity.getActive() != null && entity.getActive() == 1);
        return vo;
    }

    private AdminHireOptionVo toAdminVo(HireOptionEntity entity) {
        AdminHireOptionVo vo = new AdminHireOptionVo();
        vo.setHireOptionId(entity.getHireOptionId());
        vo.setCode(entity.getCode());
        vo.setDurationMinutes(entity.getDurationMinutes());
        vo.setBasePrice(entity.getBasePrice());
        vo.setActive(entity.getActive() != null && entity.getActive() == 1);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private HireOptionEntity findHireOptionForAdmin(String hireOptionId) {
        HireOptionEntity entity = hireOptionMapper.selectOne(new LambdaQueryWrapper<HireOptionEntity>()
                .eq(HireOptionEntity::getHireOptionId, hireOptionId));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Hire option not found");
        }
        return entity;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
