package com.lcyhz.urbanova.dto.admin.scootertype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateScooterTypeRequest {
    @NotBlank(message = "typeCode is required")
    @Size(max = 40, message = "typeCode length must not exceed 40")
    private String typeCode;

    @NotBlank(message = "displayName is required")
    @Size(max = 80, message = "displayName length must not exceed 80")
    private String displayName;

    @NotBlank(message = "imageUrl is required")
    @Size(max = 255, message = "imageUrl length must not exceed 255")
    private String imageUrl;

    @Size(max = 255, message = "description length must not exceed 255")
    private String description;
}
