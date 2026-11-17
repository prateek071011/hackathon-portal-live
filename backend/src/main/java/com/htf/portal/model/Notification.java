package com.htf.portal.model;

/**
 * One row of notifications: a short message for one user, read or not yet read.
 */
public class Notification extends BaseEntity {

    private Long userId;
    private String message;
    private boolean read;

    public Notification() {
    }

    public Notification(Long userId, String message) {
        this.userId = userId;
        this.message = message;
        this.read = false;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
