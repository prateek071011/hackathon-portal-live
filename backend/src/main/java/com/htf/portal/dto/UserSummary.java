package com.htf.portal.dto;

import com.htf.portal.model.Role;
import com.htf.portal.model.User;

/**
 * The small, safe description of the logged-in user that the frontend keeps after login.
 * It never contains the password hash.
 */
public record UserSummary(Long id, String name, String email, Role role, Long collegeId, String userCode) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getCollegeId(), user.getUserCode());
    }
}
