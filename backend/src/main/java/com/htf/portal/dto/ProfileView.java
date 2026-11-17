package com.htf.portal.dto;

import com.htf.portal.model.Role;
import com.htf.portal.model.User;

/**
 * Everything the profile page shows. hasPicture tells the page whether to download the picture.
 */
public record ProfileView(Long id, String name, String email, Role role, String userCode,
                          Long collegeId, String collegeName, String rollNumber, String branch,
                          Integer year, String skills, boolean hasPicture) {

    public static ProfileView from(User user, String collegeName) {
        return new ProfileView(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getUserCode(),
                user.getCollegeId(), collegeName, user.getRollNumber(), user.getBranch(),
                user.getStudyYear(), user.getSkills(), user.hasPicture());
    }
}
