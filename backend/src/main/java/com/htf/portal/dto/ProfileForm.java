package com.htf.portal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body of PUT /api/profile: the fields a student may change.
 * Email, college and user code are fixed after signup.
 */
public record ProfileForm(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String rollNumber,
        @NotBlank @Size(max = 60) String branch,
        @NotNull @Min(1) @Max(5) Integer year,
        @Size(max = 500) String skills) {
}
