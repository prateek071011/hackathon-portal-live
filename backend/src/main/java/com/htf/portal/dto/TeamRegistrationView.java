package com.htf.portal.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.htf.portal.model.RegistrationStatus;

/**
 * The registration part of a team page: the team's latest submission and, once approved,
 * the snapshot read back from its .ser file (snapshot is null before approval).
 */
public record TeamRegistrationView(Long id, RegistrationStatus status, String collegeName, String registrationCode,
                                   String rejectReason, LocalDateTime submittedAt, LocalDateTime decidedAt,
                                   Snapshot snapshot) {

    // leaderEmail is always null here: it is a transient field, so it was never written to the file.
    public record Snapshot(String fileName, String registrationCode, String teamName, String hackathonTitle,
                           String collegeName, List<String> memberNames, LocalDateTime approvedAt,
                           String leaderEmail) {
    }
}
