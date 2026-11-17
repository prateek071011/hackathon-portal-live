package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the logged-in user is not allowed to do this particular thing (HTTP 403),
 * e.g. a member who is not the leader tries to remove someone from the team.
 */
public class ForbiddenActionException extends PortalException {

    public ForbiddenActionException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
