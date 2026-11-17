package com.htf.portal.exception;

/**
 * A DatabaseException for MySQL error 1062 "Duplicate entry": a UNIQUE constraint said no.
 * Services catch this and turn it into a clear business error, e.g. AlreadyInTeamException.
 */
public class DuplicateKeyException extends DatabaseException {

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
