package com.lcyhz.urbanova.dto.admin.scooter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateScooterRequest {
    @NotBlank(message = "scooterId is required")
    @Size(max = 32, message = "scooterId length must not exceed 32")
    private String scooterId;

    private String typeCode;

    @Size(max = 20, message = "status length must not exceed 20")
    private String status;

    @Min(value = 0, message = "batteryPercent must be between 0 and 100")
    @Max(value = 100, message = "batteryPercent must be between 0 and 100")
    private Integer batteryPercent;

    private BigDecimal lat;
    private BigDecimal lng;

    @Size(max = 80, message = "zone length must not exceed 80")
    private String zone;

    private String color;
}
