package com.htf.portal.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.AdminTeamsView;
import com.htf.portal.dto.ApiResponse;
import com.htf.portal.service.TeamService;

/**
 * Admin-only team endpoints: the overview page and deleting any team.
 */
@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamService teamService;

    public AdminTeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ApiResponse<AdminTeamsView> overview() {
        return ApiResponse.ok(teamService.adminOverview());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        teamService.adminDeleteTeam(id);
        return ApiResponse.ok("Team deleted", null);
    }
}
