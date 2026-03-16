package com.lcyhz.urbanova.dto.booking;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateBookingRequest {
    private String scooterId;
    private String hireOptionId;
    private LocalDateTime plannedStartAt;

    public boolean hasAnyField() {
        return isNotBlank(scooterId) || isNotBlank(hireOptionId) || plannedStartAt != null;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
