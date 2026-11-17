package com.htf.portal.dto;

/**
 * One team member as the team page shows them.
 */
public record MemberView(Long userId, String name, String userCode, String branch, Integer year,
                         String skills, boolean leader, boolean hasPicture) {
}
