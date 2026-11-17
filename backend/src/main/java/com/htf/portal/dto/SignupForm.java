package com.htf.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/auth/signup. The annotations are checked by @Valid in the controller
 * before the service runs; a broken rule becomes a 400 VALIDATION_FAILED error.
 */
public record SignupForm(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        // BCrypt only uses the first 72 bytes of a password, so we cap it there.
        @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
        @NotNull Long collegeId,
        @NotBlank @Size(max = 30) String rollNumber,
        @NotBlank @Size(max = 60) String branch,
        @NotNull @Min(1) @Max(5) Integer year,
        @Size(max = 500) String skills) {
}
