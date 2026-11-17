package com.htf.portal.dao;

import java.util.Optional;

import com.htf.portal.model.College;

/**
 * Database access for the colleges table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface CollegeDao extends GenericDao<College, Long> {

    Optional<College> findByName(String name);
}
