package com.lcyhz.urbanova.dto.booking;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelBookingRequest {
    @Size(max = 255, message = "reason length must not exceed 255")
    private String reason;
}

