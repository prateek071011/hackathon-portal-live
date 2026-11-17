package com.htf.portal.exception;

/**
 * Thrown when someone tries to join a team that already has maxTeamSize members.
 */
public class TeamFullException extends ConflictException {

    public TeamFullException(String teamName) {
        super("Team '" + teamName + "' is already full", "TEAM_FULL");
    }
}
