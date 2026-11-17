package com.htf.portal.dto;

import java.time.LocalDate;

import com.htf.portal.model.Hackathon;
import com.htf.portal.model.HackathonStatus;
import com.htf.portal.util.DateUtils;

/**
 * A hackathon as the pages show it: the stored fields plus things worked out for "today":
 * status, a friendly deadline text, and how many teams it has.
 */
public record HackathonView(Long id, String title, String organizer, String description,
                            LocalDate startDate, LocalDate endDate, LocalDate registrationDeadline,
                            int minTeamSize, int maxTeamSize,
                            HackathonStatus status, String deadlineText, int teamCount) {

    public static HackathonView from(Hackathon h, LocalDate today, int teamCount) {
        return new HackathonView(h.getId(), h.getTitle(), h.getOrganizer(), h.getDescription(),
                h.getStartDate(), h.getEndDate(), h.getRegistrationDeadline(),
                h.getMinTeamSize(), h.getMaxTeamSize(),
                h.statusOn(today), DateUtils.describeDeadline(h.getRegistrationDeadline(), today), teamCount);
    }
}
