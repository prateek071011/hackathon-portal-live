package com.htf.portal.dao;

import java.util.List;
import java.util.Optional;

import com.htf.portal.model.Registration;
import com.htf.portal.model.RegistrationStatus;

/**
 * Database access for the registrations table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface RegistrationDao extends GenericDao<Registration, Long> {

    // SELECT ... FOR UPDATE, so two reviewers cannot approve and reject the same row at once.
    Optional<Registration> findByIdForUpdate(Long id);

    List<Registration> findByCollegeAndStatus(Long collegeId, RegistrationStatus status);

    // The newest submission of a team (a rejected team may have older rows).
    Optional<Registration> findLatestByTeam(Long teamId);
}
