package com.htf.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body for creating or editing a team. lookingFor is comma-separated skills, e.g. "react, figma".
 */
public record TeamForm(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @Size(max = 500) String lookingFor) {
}
