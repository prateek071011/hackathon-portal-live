package com.htf.portal.dto;

import java.util.List;

import com.htf.portal.model.TeamStatus;

/**
 * Everything the team page needs. The "...Reason" fields are null when the action is allowed;
 * otherwise they hold the sentence the page shows next to the disabled button.
 */
public record TeamView(
        Long id, String name, String description, String lookingFor, TeamStatus status,
        Long hackathonId, String hackathonTitle, int minTeamSize, int maxTeamSize, String deadlineText,
        List<MemberView> members,
        TeamViewerRole viewerRole,
        int matchPercent,
        String lockedReason,           // why the roster can no longer change (submitted / registered)
        String inviteBlockedReason,    // leader only: why inviting is off
        String applyBlockedReason,     // visitor only: why applying is off
        String submitBlockedReason,    // leader only: why "Submit to college" is off
        List<RequestView> pendingRequests,     // leader only: the team's pending invites and applications
        TeamRegistrationView registration) {   // leader and members: latest submission to the college, or null
}
