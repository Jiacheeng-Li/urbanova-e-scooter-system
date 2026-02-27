package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;

import java.util.List;

public interface HireOptionService {
    List<HireOptionVo> listActiveHireOptions();

    PriceQuoteVo quotePrice(PriceQuoteRequest request);
}

