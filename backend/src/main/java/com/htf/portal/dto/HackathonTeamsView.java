package com.htf.portal.dto;

import java.util.List;

/**
 * The teams part of a hackathon page for one student: their own team (if any), why they cannot
 * create or join a team (null if they can), the open teams sorted by match %, whether they are on the
 * "looking for a team" list, and how many students are on that list.
 */
public record HackathonTeamsView(Long myTeamId, String myTeamName, String joinBlockedReason,
                                 List<OpenTeamView> openTeams, boolean lookingForTeam, int seekerCount) {
}
