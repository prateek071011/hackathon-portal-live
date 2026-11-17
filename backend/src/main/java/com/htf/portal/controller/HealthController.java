package com.htf.portal.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.HealthInfo;

/**
 * GET /api/health: a tiny "is the backend up?" endpoint.
 * The React app calls it on its start page, and it shows the ApiResponse wrapper in action.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final PortalConfig portalConfig;

    public HealthController(PortalConfig portalConfig) {
        this.portalConfig = portalConfig;
    }

    @GetMapping
    public ApiResponse<HealthInfo> health() {
        HealthInfo info = new HealthInfo("UP", portalConfig.getUserCodePrefix(), LocalDateTime.now());
        return ApiResponse.ok("Backend is running", info);
    }
}
