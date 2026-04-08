package com.lcyhz.urbanova.dto.admin.hire;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateHireOptionRequest {
    @NotBlank(message = "code is required")
    @Size(max = 10, message = "code length must not exceed 10")
    private String code;

    @NotNull(message = "durationMinutes is required")
    @Positive(message = "durationMinutes must be greater than 0")
    private Integer durationMinutes;

    @NotNull(message = "basePrice is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "basePrice must be greater than 0")
    private BigDecimal basePrice;
}
