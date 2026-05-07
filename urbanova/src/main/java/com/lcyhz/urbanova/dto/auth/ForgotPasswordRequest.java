package com.lcyhz.urbanova.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;
}
