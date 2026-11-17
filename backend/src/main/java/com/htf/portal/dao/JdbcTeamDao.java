package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.Team;
import com.htf.portal.model.TeamStatus;

/**
 * Plain-JDBC version of TeamDao. findByIdForUpdate() is the row lock used by the accept-request transaction.
 */
@Repository
public class JdbcTeamDao extends AbstractJdbcDao<Team> implements TeamDao {

    // The "t." prefix lets the same column list work in queries that join other tables.
    private static final String COLUMNS = "t.id, t.hackathon_id, t.leader_id, t.name, t.description, "
            + "t.looking_for, t.status, t.created_at";

    public JdbcTeamDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected Team mapRow(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setId(rs.getLong("id"));
        team.setHackathonId(rs.getLong("hackathon_id"));
        team.setLeaderId(rs.getLong("leader_id"));
        team.setName(rs.getString("name"));
        team.setDescription(rs.getString("description"));
        team.setLookingFor(rs.getString("looking_for"));
        team.setStatus(TeamStatus.valueOf(rs.getString("status")));
        team.setCreatedAt(getDateTime(rs, "created_at"));
        return team;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM teams t WHERE t.id = ?", id);
    }

    @Override
    public Optional<Team> findByIdForUpdate(Long id) {
        // Another transaction that asks for the same lock waits here until we commit or roll back.
        return queryForOne("SELECT " + COLUMNS + " FROM teams t WHERE t.id = ? FOR UPDATE", id);
    }

    @Override
    public List<Team> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM teams t ORDER BY t.created_at DESC");
    }

    @Override
    public List<Team> findOpenByHackathon(Long hackathonId) {
        return queryForList("SELECT " + COLUMNS + " FROM teams t WHERE t.hackathon_id = ? AND t.status = ?",
                hackathonId, TeamStatus.OPEN);
    }

    @Override
    public List<Team> findByMember(Long userId) {
        String sql = "SELECT " + COLUMNS + " FROM teams t JOIN team_members m ON m.team_id = t.id "
                + "WHERE m.user_id = ? ORDER BY t.created_at DESC";
        return queryForList(sql, userId);
    }

    @Override
    public Optional<Team> findTeamOfUserInHackathon(Long hackathonId, Long userId) {
        String sql = "SELECT " + COLUMNS + " FROM teams t JOIN team_members m ON m.team_id = t.id "
                + "WHERE m.hackathon_id = ? AND m.user_id = ?";
        return queryForOne(sql, hackathonId, userId);
    }

    @Override
    public boolean existsByName(Long hackathonId, String name) {
        return queryForLong("SELECT COUNT(*) FROM teams WHERE hackathon_id = ? AND name = ?", hackathonId, name) > 0;
    }

    @Override
    public Team insert(Team team) {
        String sql = "INSERT INTO teams (hackathon_id, leader_id, name, description, looking_for, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        long id = insertAndGetId(sql, team.getHackathonId(), team.getLeaderId(), team.getName(),
                team.getDescription(), team.getLookingFor(), team.getStatus());
        team.setId(id);
        return team;
    }

    // Updates the details the leader can edit. Status changes go through updateStatus().
    @Override
    public boolean update(Team team) {
        String sql = "UPDATE teams SET name = ?, description = ?, looking_for = ? WHERE id = ?";
        return executeUpdate(sql, team.getName(), team.getDescription(), team.getLookingFor(), team.getId()) == 1;
    }

    @Override
    public boolean updateStatus(Long teamId, TeamStatus status) {
        return executeUpdate("UPDATE teams SET status = ? WHERE id = ?", status, teamId) == 1;
    }

    // ON DELETE CASCADE in schema.sql also removes the team's members, requests and registrations.
    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM teams WHERE id = ?", id) == 1;
    }
}
