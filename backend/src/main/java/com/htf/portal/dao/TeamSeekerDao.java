package com.htf.portal.dao;

import java.util.List;

import com.htf.portal.model.TeamSeeker;

/**
 * Database access for the team_seekers table (students looking for a team).
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface TeamSeekerDao extends GenericDao<TeamSeeker, Long> {

    // Students on the list for this hackathon who are not in a team yet, earliest first.
    List<TeamSeeker> findAvailableByHackathon(Long hackathonId);

    long countAvailableByHackathon(Long hackathonId);

    boolean exists(Long hackathonId, Long userId);

    boolean deleteByHackathonAndUser(Long hackathonId, Long userId);
}
