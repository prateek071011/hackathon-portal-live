package com.htf.portal.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.htf.portal.model.TeamStatus;

/**
 * The admin Teams page: every team, team counts per hackathon, and the last 20 actions.
 * The two small records below are only used inside this view, so they are nested here.
 */
public record AdminTeamsView(List<Row> teams, List<HackathonCount> teamsPerHackathon, List<String> recentActions) {

    public record Row(Long id, String name, String hackathonTitle, String leaderName, int memberCount,
                      int maxTeamSize, TeamStatus status, LocalDateTime createdAt) {
    }

    public record HackathonCount(String hackathonTitle, int teamCount) {
    }
}
