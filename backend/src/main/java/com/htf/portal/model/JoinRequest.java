package com.htf.portal.model;

import java.time.LocalDateTime;

/**
 * One row of join_requests: an INVITE (leader -> student) or an APPLICATION (student -> team).
 * studentId is always the student who would join. The type tells us who sent it and who must answer.
 */
public class JoinRequest extends BaseEntity {

    private Long teamId;
    private Long hackathonId;
    private Long studentId;
    private RequestType type;
    private RequestStatus status;
    private LocalDateTime updatedAt;

    public JoinRequest() {
    }

    // Overloaded constructor for a new request: it always starts PENDING.
    public JoinRequest(Long teamId, Long hackathonId, Long studentId, RequestType type) {
        this.teamId = teamId;
        this.hackathonId = hackathonId;
        this.studentId = studentId;
        this.type = type;
        this.status = RequestStatus.PENDING;
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    // INVITE: the student answers. APPLICATION: the team leader answers.
    public boolean isAnsweredBy(Long userId, Team team) {
        if (type == RequestType.INVITE) {
            return studentId.equals(userId);
        }
        return team.isLeader(userId);
    }

    // INVITE: the leader sent it. APPLICATION: the student sent it.
    public boolean isSentBy(Long userId, Team team) {
        if (type == RequestType.INVITE) {
            return team.isLeader(userId);
        }
        return studentId.equals(userId);
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
