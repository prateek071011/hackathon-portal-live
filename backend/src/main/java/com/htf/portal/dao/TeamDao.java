package com.htf.portal.dao;

import java.util.List;
import java.util.Optional;

import com.htf.portal.model.Team;
import com.htf.portal.model.TeamStatus;

/**
 * Database access for the teams table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface TeamDao extends GenericDao<Team, Long> {

    // SELECT ... FOR UPDATE: locks the row until the current transaction ends. Only call inside @Transactional.
    Optional<Team> findByIdForUpdate(Long id);

    List<Team> findOpenByHackathon(Long hackathonId);

    List<Team> findByMember(Long userId);

    Optional<Team> findTeamOfUserInHackathon(Long hackathonId, Long userId);

    boolean existsByName(Long hackathonId, String name);

    boolean updateStatus(Long teamId, TeamStatus status);
}
