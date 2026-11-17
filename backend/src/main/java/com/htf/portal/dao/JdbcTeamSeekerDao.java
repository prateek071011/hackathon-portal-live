package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.TeamSeeker;

/**
 * Plain-JDBC version of TeamSeekerDao. Like a membership, a seeker row is only inserted or deleted.
 */
@Repository
public class JdbcTeamSeekerDao extends AbstractJdbcDao<TeamSeeker> implements TeamSeekerDao {

    private static final String COLUMNS = "s.id, s.hackathon_id, s.user_id, s.created_at";

    // Safety net: even if a row was left behind, someone already in a team for this hackathon is never listed.
    private static final String AVAILABLE = "FROM team_seekers s WHERE s.hackathon_id = ? AND NOT EXISTS "
            + "(SELECT 1 FROM team_members m WHERE m.hackathon_id = s.hackathon_id AND m.user_id = s.user_id)";

    public JdbcTeamSeekerDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected TeamSeeker mapRow(ResultSet rs) throws SQLException {
        TeamSeeker seeker = new TeamSeeker(rs.getLong("hackathon_id"), rs.getLong("user_id"));
        seeker.setId(rs.getLong("id"));
        seeker.setCreatedAt(getDateTime(rs, "created_at"));
        return seeker;
    }

    @Override
    public Optional<TeamSeeker> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM team_seekers s WHERE s.id = ?", id);
    }

    @Override
    public List<TeamSeeker> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM team_seekers s ORDER BY s.created_at");
    }

    @Override
    public List<TeamSeeker> findAvailableByHackathon(Long hackathonId) {
        return queryForList("SELECT " + COLUMNS + " " + AVAILABLE + " ORDER BY s.created_at", hackathonId);
    }

    @Override
    public long countAvailableByHackathon(Long hackathonId) {
        return queryForLong("SELECT COUNT(*) " + AVAILABLE, hackathonId);
    }

    @Override
    public boolean exists(Long hackathonId, Long userId) {
        return queryForLong("SELECT COUNT(*) FROM team_seekers WHERE hackathon_id = ? AND user_id = ?",
                hackathonId, userId) > 0;
    }

    @Override
    public TeamSeeker insert(TeamSeeker seeker) {
        long id = insertAndGetId("INSERT INTO team_seekers (hackathon_id, user_id) VALUES (?, ?)",
                seeker.getHackathonId(), seeker.getUserId());
        seeker.setId(id);
        return seeker;
    }

    // A seeker row has nothing to change: students join the list (insert) or leave it (delete).
    @Override
    public boolean update(TeamSeeker seeker) {
        throw new UnsupportedOperationException("Seeker rows are only inserted or deleted");
    }

    @Override
    public boolean deleteByHackathonAndUser(Long hackathonId, Long userId) {
        return executeUpdate("DELETE FROM team_seekers WHERE hackathon_id = ? AND user_id = ?", hackathonId, userId) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM team_seekers WHERE id = ?", id) == 1;
    }
}
