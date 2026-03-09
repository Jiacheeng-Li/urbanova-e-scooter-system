package com.lcyhz.urbanova.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {
    @NotBlank(message = "scooterId is required")
    private String scooterId;

    @NotBlank(message = "hireOptionId is required")
    private String hireOptionId;

    private LocalDateTime plannedStartAt;
}

