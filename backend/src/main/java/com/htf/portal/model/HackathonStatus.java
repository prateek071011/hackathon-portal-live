package com.htf.portal.model;

/**
 * Where a hackathon is in its life. NOT stored in the database: it is worked out
 * from today's date and the hackathon's deadline / start / end dates.
 */
public enum HackathonStatus {
    OPEN,       // today is on or before the registration deadline
    CLOSED,     // deadline passed, event not started yet
    ONGOING,    // between start date and end date
    COMPLETED   // after the end date
}
