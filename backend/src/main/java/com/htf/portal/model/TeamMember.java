package com.htf.portal.model;

/**
 * One row of team_members: "this user is in this team". hackathonId is copied from the team on purpose,
 * so the database rule UNIQUE(hackathon_id, user_id) can stop anyone joining two teams in one hackathon.
 */
public class TeamMember extends BaseEntity {

    private Long teamId;
    private Long hackathonId;
    private Long userId;

    public TeamMember() {
    }

    public TeamMember(Long teamId, Long hackathonId, Long userId) {
        this.teamId = teamId;
        this.hackathonId = hackathonId;
        this.userId = userId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getHackathonId() {
        return hackathonId;
    }

    public void setHackathonId(Long hackathonId) {
        this.hackathonId = hackathonId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
