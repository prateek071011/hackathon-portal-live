package com.htf.portal.exception;

/**
 * Thrown when a student is already in a team for this hackathon (one team per hackathon).
 * Also used when the database's UNIQUE(hackathon_id, user_id) rule rejects an insert.
 */
public class AlreadyInTeamException extends ConflictException {

    public AlreadyInTeamException(String message) {
        super(message, "ALREADY_IN_TEAM");
    }

    public AlreadyInTeamException() {
        this("This student is already in a team for this hackathon");
    }
}
