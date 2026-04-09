package com.lcyhz.urbanova.dto.admin.scootertype;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateScooterTypeRequest {
    @Size(max = 80, message = "displayName length must not exceed 80")
    private String displayName;

    @Size(max = 255, message = "imageUrl length must not exceed 255")
    private String imageUrl;

    @Size(max = 255, message = "description length must not exceed 255")
    private String description;

    public boolean hasAnyField() {
        return displayName != null || imageUrl != null || description != null;
    }
}
