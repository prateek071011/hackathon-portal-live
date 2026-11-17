package com.htf.portal.dao;

import java.util.List;
import java.util.Map;

import com.htf.portal.model.Hackathon;

/**
 * Database access for the hackathons table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface HackathonDao extends GenericDao<Hackathon, Long> {

    long countTeams(Long hackathonId);

    // hackathonId -> number of teams. Hackathons with no teams are not in the map.
    Map<Long, Integer> countTeamsPerHackathon();

    // Inserts all rows in ONE manual transaction: either every row is saved or none is.
    int insertAll(List<Hackathon> hackathons);
}
