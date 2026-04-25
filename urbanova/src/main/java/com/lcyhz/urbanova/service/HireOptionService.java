package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.admin.hire.CreateHireOptionRequest;
import com.lcyhz.urbanova.dto.admin.hire.UpdateHireOptionRequest;
import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.vo.hire.AdminHireOptionVo;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;

import java.util.List;

public interface HireOptionService {
    List<HireOptionVo> listActiveHireOptions();

    PriceQuoteVo quotePrice(String userId, PriceQuoteRequest request);

    List<AdminHireOptionVo> listAllHireOptions();

    AdminHireOptionVo createHireOption(CreateHireOptionRequest request);

    AdminHireOptionVo updateHireOption(String hireOptionId, UpdateHireOptionRequest request);

    AdminHireOptionVo disableHireOption(String hireOptionId);
}
