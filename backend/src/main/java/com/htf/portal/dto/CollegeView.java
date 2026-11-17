package com.htf.portal.dto;

/**
 * A college on the admin page, with the reviewer account that logs in for it (null if it has none).
 */
public record CollegeView(Long id, String name, String reviewerName, String reviewerEmail) {
}
