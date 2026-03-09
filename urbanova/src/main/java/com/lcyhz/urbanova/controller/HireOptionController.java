package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.service.HireOptionService;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HireOptionController {
    private final HireOptionService hireOptionService;

    @GetMapping("/hire-options")
    public ApiResponse<List<HireOptionVo>> listHireOptions() {
        return ApiResponse.success(hireOptionService.listActiveHireOptions());
    }

    @PostMapping("/pricing/quotes")
    public ApiResponse<PriceQuoteVo> quote(@Valid @RequestBody PriceQuoteRequest request) {
        return ApiResponse.success(hireOptionService.quotePrice(request));
    }
}

