package com.lcyhz.urbanova.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PriceQuoteRequest {
    private String scooterId;

    @NotBlank(message = "hireOptionCode is required")
    private String hireOptionCode;
}

