package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.service.HireOptionService;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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

    private HireOptionVo toVo(HireOptionEntity entity) {
        HireOptionVo vo = new HireOptionVo();
        vo.setHireOptionId(entity.getHireOptionId());
        vo.setCode(entity.getCode());
        vo.setDurationMinutes(entity.getDurationMinutes());
        vo.setBasePrice(entity.getBasePrice());
        vo.setActive(entity.getActive() != null && entity.getActive() == 1);
        return vo;
    }
}

