package com.htf.portal.dto;

import com.htf.portal.model.TeamStatus;

/**
 * A team the student belongs to, for the "Your teams" box on the hackathons page.
 */
public record MyTeamView(Long id, String name, TeamStatus status, Long hackathonId, String hackathonTitle,
                         boolean leader) {
}
