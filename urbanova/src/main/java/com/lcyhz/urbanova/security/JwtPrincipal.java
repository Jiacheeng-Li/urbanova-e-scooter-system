package com.lcyhz.urbanova.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtPrincipal {
    private String userId;
    private String role;
    private String email;
}

