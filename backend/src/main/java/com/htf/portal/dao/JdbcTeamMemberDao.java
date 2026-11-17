package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.TeamMember;

/**
 * Plain-JDBC version of TeamMemberDao. A membership row is only ever inserted or deleted, never changed.
 */
@Repository
public class JdbcTeamMemberDao extends AbstractJdbcDao<TeamMember> implements TeamMemberDao {

    private static final String COLUMNS = "id, team_id, hackathon_id, user_id, created_at";

    public JdbcTeamMemberDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected TeamMember mapRow(ResultSet rs) throws SQLException {
        TeamMember member = new TeamMember(rs.getLong("team_id"), rs.getLong("hackathon_id"), rs.getLong("user_id"));
        member.setId(rs.getLong("id"));
        member.setCreatedAt(getDateTime(rs, "created_at"));
        return member;
    }

    @Override
    public Optional<TeamMember> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM team_members WHERE id = ?", id);
    }

    @Override
    public List<TeamMember> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM team_members ORDER BY id");
    }

    // Oldest first, so the leader (who joined first) is at the top.
    @Override
    public List<TeamMember> findByTeam(Long teamId) {
        return queryForList("SELECT " + COLUMNS + " FROM team_members WHERE team_id = ? ORDER BY created_at, id", teamId);
    }

    @Override
    public long countByTeam(Long teamId) {
        return queryForLong("SELECT COUNT(*) FROM team_members WHERE team_id = ?", teamId);
    }

    @Override
    public Map<Long, Integer> countMembersPerTeam(Long hackathonId) {
        return queryForCounts("SELECT team_id, COUNT(*) FROM team_members WHERE hackathon_id = ? GROUP BY team_id",
                hackathonId);
    }

    @Override
    public Map<Long, Integer> countMembersPerTeam() {
        return queryForCounts("SELECT team_id, COUNT(*) FROM team_members GROUP BY team_id");
    }

    @Override
    public boolean existsByHackathonAndUser(Long hackathonId, Long userId) {
        return queryForLong("SELECT COUNT(*) FROM team_members WHERE hackathon_id = ? AND user_id = ?",
                hackathonId, userId) > 0;
    }

    // If the student is already in a team for this hackathon, MySQL refuses the insert
    // (UNIQUE(hackathon_id, user_id)) and AbstractJdbcDao throws DuplicateKeyException.
    @Override
    public TeamMember insert(TeamMember member) {
        long id = insertAndGetId("INSERT INTO team_members (team_id, hackathon_id, user_id) VALUES (?, ?, ?)",
                member.getTeamId(), member.getHackathonId(), member.getUserId());
        member.setId(id);
        return member;
    }

    // GenericDao asks every DAO for update(), but a membership has nothing to change:
    // people join (insert) or leave (delete). So this operation is deliberately not supported.
    @Override
    public boolean update(TeamMember member) {
        throw new UnsupportedOperationException("Team memberships are only inserted or deleted");
    }

    @Override
    public boolean deleteByTeamAndUser(Long teamId, Long userId) {
        return executeUpdate("DELETE FROM team_members WHERE team_id = ? AND user_id = ?", teamId, userId) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM team_members WHERE id = ?", id) == 1;
    }
}
