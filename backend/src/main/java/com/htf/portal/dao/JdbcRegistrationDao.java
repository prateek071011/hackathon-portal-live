package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.Registration;
import com.htf.portal.model.RegistrationStatus;

/**
 * Plain-JDBC version of RegistrationDao.
 */
@Repository
public class JdbcRegistrationDao extends AbstractJdbcDao<Registration> implements RegistrationDao {

    private static final String COLUMNS = "id, team_id, college_id, status, registration_code, reject_reason, "
            + "decided_at, created_at";

    public JdbcRegistrationDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected Registration mapRow(ResultSet rs) throws SQLException {
        Registration r = new Registration(rs.getLong("team_id"), rs.getLong("college_id"));
        r.setId(rs.getLong("id"));
        r.setStatus(RegistrationStatus.valueOf(rs.getString("status")));
        r.setRegistrationCode(rs.getString("registration_code"));
        r.setRejectReason(rs.getString("reject_reason"));
        r.setDecidedAt(getDateTime(rs, "decided_at"));
        r.setCreatedAt(getDateTime(rs, "created_at"));
        return r;
    }

    @Override
    public Optional<Registration> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM registrations WHERE id = ?", id);
    }

    @Override
    public Optional<Registration> findByIdForUpdate(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM registrations WHERE id = ? FOR UPDATE", id);
    }

    @Override
    public List<Registration> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM registrations ORDER BY created_at");
    }

    // Oldest submission first, so the queue is first come, first served.
    @Override
    public List<Registration> findByCollegeAndStatus(Long collegeId, RegistrationStatus status) {
        return queryForList("SELECT " + COLUMNS + " FROM registrations WHERE college_id = ? AND status = ? ORDER BY created_at, id",
                collegeId, status);
    }

    @Override
    public Optional<Registration> findLatestByTeam(Long teamId) {
        return queryForOne("SELECT " + COLUMNS + " FROM registrations WHERE team_id = ? ORDER BY id DESC LIMIT 1", teamId);
    }

    @Override
    public Registration insert(Registration r) {
        long id = insertAndGetId("INSERT INTO registrations (team_id, college_id, status) VALUES (?, ?, ?)",
                r.getTeamId(), r.getCollegeId(), r.getStatus());
        r.setId(id);
        return r;
    }

    // Saves the college's decision.
    @Override
    public boolean update(Registration r) {
        String sql = "UPDATE registrations SET status = ?, registration_code = ?, reject_reason = ?, decided_at = ? WHERE id = ?";
        return executeUpdate(sql, r.getStatus(), r.getRegistrationCode(), r.getRejectReason(), r.getDecidedAt(), r.getId()) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM registrations WHERE id = ?", id) == 1;
    }
}
