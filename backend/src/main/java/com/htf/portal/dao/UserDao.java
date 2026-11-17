package com.htf.portal.dao;

import java.util.List;
import java.util.Optional;

import com.htf.portal.model.Role;
import com.htf.portal.model.User;

/**
 * Database access for the users table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface UserDao extends GenericDao<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserCode(String userCode);

    List<User> findByRole(Role role);

    boolean existsByEmail(String email);

    boolean existsByUserCode(String userCode);

    // Only changes the picture column, so a profile edit and a picture upload never overwrite each other.
    boolean updatePicture(Long userId, String pictureFile);
}
