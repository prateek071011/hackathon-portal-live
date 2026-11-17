package com.htf.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/teams/{id}/invites: one or more user codes, separated by commas or spaces.
 */
public record InviteForm(@NotBlank @Size(max = 500) String userCodes) {
}
