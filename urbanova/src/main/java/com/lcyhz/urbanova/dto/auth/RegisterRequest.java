package com.lcyhz.urbanova.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password length must be between 8 and 72")
    private String password;

    @NotBlank(message = "fullName is required")
    @Size(max = 100, message = "fullName length must not exceed 100")
    private String fullName;

    @Size(max = 30, message = "phone length must not exceed 30")
    private String phone;
}

