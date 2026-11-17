package com.htf.portal.model;

/**
 * Life of a team: OPEN -> FULL -> SUBMITTED -> REGISTERED. Stored in teams.status.
 */
public enum TeamStatus {
    OPEN,        // listed, accepting people
    FULL,        // members == maxTeamSize (set automatically)
    SUBMITTED,   // sent to the leader's college, roster locked
    REGISTERED   // college approved
}
