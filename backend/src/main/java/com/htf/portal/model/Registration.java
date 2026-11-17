package com.htf.portal.model;

import java.time.LocalDateTime;

/**
 * One row of registrations: a team sent to the leader's college for approval.
 * createdAt is when it was submitted. A rejected team can submit again, which adds a new row.
 */
public class Registration extends BaseEntity {

    private Long teamId;
    private Long collegeId;
    private RegistrationStatus status;
    private String registrationCode;   // set on approval, e.g. REG-2026-000123
    private String rejectReason;
    private LocalDateTime decidedAt;

    public Registration() {
    }

    // Overloaded constructor for a new submission: it always starts PENDING.
    public Registration(Long teamId, Long collegeId) {
        this.teamId = teamId;
        this.collegeId = collegeId;
        this.status = RegistrationStatus.PENDING;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
