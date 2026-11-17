package com.htf.portal.model;

import java.time.LocalDateTime;

/**
 * One row of the colleges table. Students pick their college at signup,
 * and each college has a COLLEGE login account that reviews its teams.
 */
public class College extends BaseEntity {

    private String name;

    public College() {
    }

    // For a new college that is not saved yet (no id).
    public College(String name) {
        this.name = name;
    }

    // For a college read from the database.
    public College(Long id, String name, LocalDateTime createdAt) {
        super(id, createdAt);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
