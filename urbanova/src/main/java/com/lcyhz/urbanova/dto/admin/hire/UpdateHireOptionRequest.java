package com.lcyhz.urbanova.dto.admin.hire;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateHireOptionRequest {
    @Positive(message = "durationMinutes must be greater than 0")
    private Integer durationMinutes;

    @DecimalMin(value = "0.0", inclusive = false, message = "basePrice must be greater than 0")
    private BigDecimal basePrice;

    public boolean hasAnyField() {
        return durationMinutes != null || basePrice != null;
    }
}
