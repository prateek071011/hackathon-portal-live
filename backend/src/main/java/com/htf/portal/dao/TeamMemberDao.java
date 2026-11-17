package com.htf.portal.dao;

import java.util.List;
import java.util.Map;

import com.htf.portal.model.TeamMember;

/**
 * Database access for the team_members table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface TeamMemberDao extends GenericDao<TeamMember, Long> {

    List<TeamMember> findByTeam(Long teamId);

    long countByTeam(Long teamId);

    // teamId -> member count, for every team of one hackathon.
    Map<Long, Integer> countMembersPerTeam(Long hackathonId);

    // teamId -> member count, for every team (admin page).
    Map<Long, Integer> countMembersPerTeam();

    boolean existsByHackathonAndUser(Long hackathonId, Long userId);

    boolean deleteByTeamAndUser(Long teamId, Long userId);
}
