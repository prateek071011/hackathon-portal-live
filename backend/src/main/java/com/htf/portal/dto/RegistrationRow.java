package com.htf.portal.dto;

import java.time.LocalDateTime;

import com.htf.portal.model.RegistrationStatus;

/**
 * One line in the college's review queue or registered-teams table.
 */
public record RegistrationRow(Long id, Long teamId, String teamName, String hackathonTitle, String leaderName,
                              int memberCount, RegistrationStatus status, String registrationCode,
                              String rejectReason, LocalDateTime submittedAt, LocalDateTime decidedAt) {
}
