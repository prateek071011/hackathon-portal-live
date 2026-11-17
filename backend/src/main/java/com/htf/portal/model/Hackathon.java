package com.htf.portal.model;

import java.time.LocalDate;

/**
 * One row of the hackathons table. The status (OPEN / CLOSED / ONGOING / COMPLETED) is not stored:
 * statusOn(today) works it out from the dates, so it is always correct without a nightly job.
 */
public class Hackathon extends BaseEntity {

    public static final int MAX_ALLOWED_TEAM_SIZE = 6;

    private String title;
    private String organizer;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;
    private int minTeamSize;
    private int maxTeamSize;

    // Used by the DAO, which fills the fields with setters while reading a row.
    public Hackathon() {
    }

    // Overloaded constructor for a new hackathon (from the admin form or a CSV line): everything except id.
    public Hackathon(String title, String organizer, String description, LocalDate startDate, LocalDate endDate,
                     LocalDate registrationDeadline, int minTeamSize, int maxTeamSize) {
        this.title = title;
        this.organizer = organizer;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationDeadline = registrationDeadline;
        this.minTeamSize = minTeamSize;
        this.maxTeamSize = maxTeamSize;
    }

    /**
     * Checks the rules from the spec and returns the first problem in plain words, or null if all is fine.
     * The admin form and the CSV import both use this, so the rules live in exactly one place.
     */
    public String checkRules() {
        if (minTeamSize < 1) {
            return "Minimum team size must be at least 1";
        }
        if (maxTeamSize > MAX_ALLOWED_TEAM_SIZE) {
            return "Maximum team size cannot be more than " + MAX_ALLOWED_TEAM_SIZE;
        }
        if (minTeamSize > maxTeamSize) {
            return "Minimum team size cannot be bigger than the maximum";
        }
        if (!registrationDeadline.isBefore(startDate)) {
            return "Registration deadline must be before the start date";
        }
        if (endDate.isBefore(startDate)) {
            return "End date cannot be before the start date";
        }
        return null;
    }

    /** Works out the status for a given day. The deadline day itself still counts as OPEN. */
    public HackathonStatus statusOn(LocalDate today) {
        if (!today.isAfter(registrationDeadline)) {
            return HackathonStatus.OPEN;
        }
        if (today.isBefore(startDate)) {
            return HackathonStatus.CLOSED;
        }
        if (!today.isAfter(endDate)) {
            return HackathonStatus.ONGOING;
        }
        return HackathonStatus.COMPLETED;
    }

    public boolean isRegistrationOpenOn(LocalDate today) {
        return statusOn(today) == HackathonStatus.OPEN;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public int getMinTeamSize() {
        return minTeamSize;
    }

    public void setMinTeamSize(int minTeamSize) {
        this.minTeamSize = minTeamSize;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }
}
