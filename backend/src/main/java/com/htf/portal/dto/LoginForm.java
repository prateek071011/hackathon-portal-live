package com.htf.portal.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/login.
 */
public record LoginForm(@NotBlank String email, @NotBlank String password) {
}
