package com.lcyhz.urbanova.dto.admin.scooter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkUpdateScooterStatusRequest {
    @NotEmpty(message = "scooterIds is required")
    private List<String> scooterIds;

    @NotBlank(message = "status is required")
    private String status;
}
