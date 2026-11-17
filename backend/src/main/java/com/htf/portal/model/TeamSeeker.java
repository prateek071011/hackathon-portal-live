package com.htf.portal.model;

/**
 * One row of team_seekers: "this student is looking for a team in this hackathon".
 * Students add themselves (opt-in), so team leaders only see people who asked to be found.
 */
public class TeamSeeker extends BaseEntity {

    private Long hackathonId;
    private Long userId;

    public TeamSeeker() {
    }

    public TeamSeeker(Long hackathonId, Long userId) {
        this.hackathonId = hackathonId;
        this.userId = userId;
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
