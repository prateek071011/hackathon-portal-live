package com.htf.portal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.HackathonView;
import com.htf.portal.service.HackathonService;

/**
 * Read-only hackathon endpoints for any logged-in user (students browse them, the admin page lists them).
 */
@RestController
@RequestMapping("/api/hackathons")
public class HackathonController {

    private final HackathonService hackathonService;

    public HackathonController(HackathonService hackathonService) {
        this.hackathonService = hackathonService;
    }

    @GetMapping
    public ApiResponse<List<HackathonView>> list() {
        return ApiResponse.ok(hackathonService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<HackathonView> get(@PathVariable Long id) {
        return ApiResponse.ok(hackathonService.getView(id));
    }
}
