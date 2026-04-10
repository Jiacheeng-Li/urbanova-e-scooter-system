package com.lcyhz.urbanova.dto.admin.scooter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateScooterRequest {
    @Min(value = 0, message = "batteryPercent must be between 0 and 100")
    @Max(value = 100, message = "batteryPercent must be between 0 and 100")
    private Integer batteryPercent;

    private String typeCode;

    private BigDecimal lat;
    private BigDecimal lng;

    @Size(max = 80, message = "zone length must not exceed 80")
    private String zone;

    private String color;

    public boolean hasAnyField() {
        return batteryPercent != null || lat != null || lng != null || zone != null;
    }
}
