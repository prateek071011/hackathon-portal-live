package com.htf.portal.exception;

/**
 * Wraps a java.sql.SQLException (which is checked) in an unchecked exception, so DAO methods
 * stay free of "throws SQLException" and a database failure inside @Transactional rolls back.
 * It is NOT a PortalException: it means something unexpected broke, so the client gets a 500.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
