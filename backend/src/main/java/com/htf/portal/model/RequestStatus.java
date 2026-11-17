package com.htf.portal.model;

/**
 * State of a join request. Only PENDING requests can be accepted, rejected or cancelled.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
