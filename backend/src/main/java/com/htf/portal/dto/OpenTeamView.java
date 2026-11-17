package com.htf.portal.dto;

import java.time.LocalDateTime;

/**
 * One OPEN team in a hackathon's team list, with the viewer's match %.
 * pendingWithMe = the viewer already has a pending invite or application with this team.
 */
public record OpenTeamView(Long id, String name, String description, String lookingFor, String leaderName,
                           int memberCount, int maxTeamSize, int matchPercent, boolean pendingWithMe,
                           LocalDateTime createdAt) {
}
