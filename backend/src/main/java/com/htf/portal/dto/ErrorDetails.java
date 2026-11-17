package com.htf.portal.dto;

/**
 * What an error looks like inside ApiResponse.data: { status, code, message }.
 * code is a short constant (e.g. TEAM_FULL) the frontend could check without reading the text.
 */
public record ErrorDetails(int status, String code, String message) {
}
