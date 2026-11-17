package com.htf.portal.model;

/**
 * The two kinds of join request. The type also tells us who sent it and who must answer it.
 */
public enum RequestType {
    INVITE,      // leader -> student (by userCode); the student answers
    APPLICATION  // student -> open team; the leader answers
}
