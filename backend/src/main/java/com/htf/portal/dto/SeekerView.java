package com.htf.portal.dto;

/**
 * One student in a leader's "Find teammates" list. Only what a leader needs to decide on an invite:
 * no email and no roll number. invitePending = this team already has a pending request with them.
 */
public record SeekerView(Long userId, String name, String userCode, String collegeName, String branch,
                         Integer year, String skills, int matchPercent, boolean invitePending) {
}
