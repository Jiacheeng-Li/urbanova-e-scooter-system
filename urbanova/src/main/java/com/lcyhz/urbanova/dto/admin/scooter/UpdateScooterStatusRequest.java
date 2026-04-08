package com.lcyhz.urbanova.dto.admin.scooter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateScooterStatusRequest {
    @NotBlank(message = "status is required")
    private String status;
}
