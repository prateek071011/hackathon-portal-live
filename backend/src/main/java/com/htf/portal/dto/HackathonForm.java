package com.htf.portal.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body for creating or editing a hackathon (admin). Dates arrive as "2026-12-01".
 * Single-field rules are annotations here. Rules that compare two fields (min <= max,
 * deadline before start) are in Hackathon.checkRules().
 */
public record HackathonForm(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 150) String organizer,
        @Size(max = 5000) String description,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LocalDate registrationDeadline,
        @NotNull @Min(1) @Max(6) Integer minTeamSize,
        @NotNull @Min(1) @Max(6) Integer maxTeamSize) {
}
