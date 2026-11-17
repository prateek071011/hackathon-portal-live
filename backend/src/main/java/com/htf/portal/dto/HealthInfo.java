package com.htf.portal.dto;

import java.time.LocalDateTime;

/**
 * Data returned by GET /api/health.
 * userCodePrefix comes from portal.properties, so it also proves PortalConfig loaded.
 */
public record HealthInfo(String status, String userCodePrefix, LocalDateTime serverTime) {
}
