package com.htf.portal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.HackathonTeamsView;
import com.htf.portal.dto.InviteForm;
import com.htf.portal.dto.InviteResult;
import com.htf.portal.dto.MyTeamView;
import com.htf.portal.dto.SeekerView;
import com.htf.portal.dto.TeamForm;
import com.htf.portal.dto.TeamView;
import com.htf.portal.security.AuthUser;
import com.htf.portal.service.RegistrationService;
import com.htf.portal.service.RequestService;
import com.htf.portal.service.SeekerService;
import com.htf.portal.service.TeamService;

import jakarta.validation.Valid;

/**
 * Team endpoints for logged-in users. Who may do what (leader? member? deadline?) is checked in the services,
 * using the user id from the token, never an id sent by the browser.
 */
@RestController
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;
    private final RequestService requestService;
    private final RegistrationService registrationService;
    private final SeekerService seekerService;

    public TeamController(TeamService teamService, RequestService requestService,
                          RegistrationService registrationService, SeekerService seekerService) {
        this.teamService = teamService;
        this.requestService = requestService;
        this.registrationService = registrationService;
        this.seekerService = seekerService;
    }

    // "I'm looking for a team": put myself on this hackathon's list, or take myself off it.
    @PostMapping("/hackathons/{hackathonId}/looking-for-team")
    public ApiResponse<Void> startLooking(@AuthenticationPrincipal AuthUser me, @PathVariable Long hackathonId) {
        return ApiResponse.ok(seekerService.startLooking(me.id(), hackathonId), null);
    }

    @DeleteMapping("/hackathons/{hackathonId}/looking-for-team")
    public ApiResponse<Void> stopLooking(@AuthenticationPrincipal AuthUser me, @PathVariable Long hackathonId) {
        return ApiResponse.ok(seekerService.stopLooking(me.id(), hackathonId), null);
    }

    // The leader's "Find teammates" list: students on the list, best match first.
    @GetMapping("/teams/{id}/seekers")
    public ApiResponse<List<SeekerView>> seekers(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(seekerService.seekersForTeam(me.id(), id));
    }

    @GetMapping("/hackathons/{hackathonId}/teams")
    public ApiResponse<HackathonTeamsView> teamsForHackathon(@AuthenticationPrincipal AuthUser me,
                                                             @PathVariable Long hackathonId) {
        return ApiResponse.ok(teamService.teamsForHackathon(me.id(), hackathonId));
    }

    @PostMapping("/hackathons/{hackathonId}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamView> create(@AuthenticationPrincipal AuthUser me, @PathVariable Long hackathonId,
                                        @Valid @RequestBody TeamForm form) {
        return ApiResponse.ok("Team created. You are the leader.", teamService.createTeam(me.id(), hackathonId, form));
    }

    @GetMapping("/teams/mine")
    public ApiResponse<List<MyTeamView>> myTeams(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(teamService.myTeams(me.id()));
    }

    @GetMapping("/teams/{id}")
    public ApiResponse<TeamView> get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(teamService.getTeam(me.id(), id));
    }

    @PutMapping("/teams/{id}")
    public ApiResponse<TeamView> update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody TeamForm form) {
        return ApiResponse.ok("Team saved", teamService.updateTeam(me.id(), id, form));
    }

    @DeleteMapping("/teams/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        teamService.deleteTeam(me.id(), id);
        return ApiResponse.ok("Team deleted", null);
    }

    @PostMapping("/teams/{id}/leave")
    public ApiResponse<Void> leave(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        teamService.leaveTeam(me.id(), id);
        return ApiResponse.ok("You left the team", null);
    }

    @DeleteMapping("/teams/{id}/members/{userId}")
    public ApiResponse<TeamView> removeMember(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                              @PathVariable Long userId) {
        return ApiResponse.ok("Member removed", teamService.removeMember(me.id(), id, userId));
    }

    @PostMapping("/teams/{id}/invites")
    public ApiResponse<InviteResult> invite(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                            @Valid @RequestBody InviteForm form) {
        InviteResult result = requestService.invite(me.id(), id, form.userCodes());
        return ApiResponse.ok("Invites sent: " + result.sent().size(), result);
    }

    @PostMapping("/teams/{id}/apply")
    public ApiResponse<Void> apply(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(requestService.apply(me.id(), id), null);
    }

    @PostMapping("/teams/{id}/submit")
    public ApiResponse<Void> submit(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(registrationService.submit(me.id(), id), null);
    }
}
