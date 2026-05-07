package com.lcyhz.urbanova.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "code is required")
    @Size(min = 6, max = 6, message = "code must be 6 digits")
    private String code;

    @NotBlank(message = "newPassword is required")
    @Size(min = 8, max = 64, message = "newPassword must be between 8 and 64 characters")
    private String newPassword;
}
