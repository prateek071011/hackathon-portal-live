package com.htf.portal.dto;

import java.util.List;

/**
 * Outcome of inviting several codes at once: who was invited, and why each other code was skipped.
 */
public record InviteResult(List<String> sent, List<String> skipped) {
}
