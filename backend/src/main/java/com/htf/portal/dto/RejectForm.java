package com.htf.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/college/registrations/{id}/reject. A reason is required: the leader is told it.
 */
public record RejectForm(@NotBlank(message = "please give a reason") @Size(max = 500) String reason) {
}
