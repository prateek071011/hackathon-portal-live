package com.htf.portal.model;

import java.time.LocalDateTime;

/**
 * Fields every table row has: an auto-increment id and the time it was created.
 * Abstract because nobody stores a "BaseEntity" on its own; User, Team, etc. extend it.
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;

    protected BaseEntity() {
    }

    protected BaseEntity(Long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
