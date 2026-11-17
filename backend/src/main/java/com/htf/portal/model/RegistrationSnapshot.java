package com.htf.portal.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A frozen copy of a team at the moment its college approved it, saved to data/snapshots/{code}.ser with Java
 * serialization. Even if members later change their names or the team is deleted, this file still says who was
 * registered. "implements Serializable" is what allows ObjectOutputStream to write it.
 */
public class RegistrationSnapshot implements Serializable {

    // Version number of this class's layout. If we change the fields later we bump it, and Java then refuses to
    // read old files with a clear InvalidClassException instead of silently mixing old and new layouts.
    @Serial
    private static final long serialVersionUID = 1L;

    private final String registrationCode;
    private final String teamName;
    private final String hackathonTitle;
    private final String collegeName;
    private final ArrayList<String> memberNames;   // ArrayList is Serializable (the List interface itself is not)
    private final LocalDateTime approvedAt;

    // transient = NOT written to the file. After reading the file back this field is null.
    // We use it for the leader's email: a personal detail the archived copy does not need to keep.
    private final transient String leaderEmail;

    public RegistrationSnapshot(String registrationCode, String teamName, String hackathonTitle, String collegeName,
                                List<String> memberNames, LocalDateTime approvedAt, String leaderEmail) {
        this.registrationCode = registrationCode;
        this.teamName = teamName;
        this.hackathonTitle = hackathonTitle;
        this.collegeName = collegeName;
        this.memberNames = new ArrayList<>(memberNames);
        this.approvedAt = approvedAt;
        this.leaderEmail = leaderEmail;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getHackathonTitle() {
        return hackathonTitle;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public List<String> getMemberNames() {
        return new ArrayList<>(memberNames);
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getLeaderEmail() {
        return leaderEmail;
    }
}
