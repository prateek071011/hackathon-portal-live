package com.htf.portal.model;

/**
 * One row of the teams table. The leader is also a row in team_members, like every other member.
 * status moves OPEN -> FULL -> SUBMITTED -> REGISTERED (see TeamStatus).
 */
public class Team extends BaseEntity {

    private Long hackathonId;
    private Long leaderId;
    private String name;
    private String description;
    private String lookingFor;   // comma-separated skills, e.g. "react, figma"
    private TeamStatus status;

    // Used by the DAO, which fills the fields with setters while reading a row.
    public Team() {
    }

    // Overloaded constructor for a brand-new team: it always starts OPEN.
    public Team(Long hackathonId, Long leaderId, String name, String description, String lookingFor) {
        this.hackathonId = hackathonId;
        this.leaderId = leaderId;
        this.name = name;
        this.description = description;
        this.lookingFor = lookingFor;
        this.status = TeamStatus.OPEN;
    }

    public boolean isLeader(Long userId) {
        return leaderId.equals(userId);
    }

    // After submission the roster is frozen: nobody joins, leaves or is removed.
    public boolean isLocked() {
        return status == TeamStatus.SUBMITTED || status == TeamStatus.REGISTERED;
    }

    public Long getHackathonId() {
        return hackathonId;
    }

    public void setHackathonId(Long hackathonId) {
        this.hackathonId = hackathonId;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLookingFor() {
        return lookingFor;
    }

    public void setLookingFor(String lookingFor) {
        this.lookingFor = lookingFor;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }
}
