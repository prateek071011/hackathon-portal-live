package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.JoinRequest;
import com.htf.portal.model.RequestStatus;
import com.htf.portal.model.RequestType;

/**
 * Plain-JDBC version of JoinRequestDao.
 */
@Repository
public class JdbcJoinRequestDao extends AbstractJdbcDao<JoinRequest> implements JoinRequestDao {

    private static final String COLUMNS = "r.id, r.team_id, r.hackathon_id, r.student_id, r.type, r.status, "
            + "r.created_at, r.updated_at";

    public JdbcJoinRequestDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected JoinRequest mapRow(ResultSet rs) throws SQLException {
        JoinRequest request = new JoinRequest();
        request.setId(rs.getLong("id"));
        request.setTeamId(rs.getLong("team_id"));
        request.setHackathonId(rs.getLong("hackathon_id"));
        request.setStudentId(rs.getLong("student_id"));
        request.setType(RequestType.valueOf(rs.getString("type")));
        request.setStatus(RequestStatus.valueOf(rs.getString("status")));
        request.setCreatedAt(getDateTime(rs, "created_at"));
        request.setUpdatedAt(getDateTime(rs, "updated_at"));
        return request;
    }

    @Override
    public Optional<JoinRequest> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM join_requests r WHERE r.id = ?", id);
    }

    @Override
    public List<JoinRequest> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM join_requests r ORDER BY r.created_at DESC");
    }

    @Override
    public boolean existsPending(Long teamId, Long studentId) {
        String sql = "SELECT COUNT(*) FROM join_requests WHERE team_id = ? AND student_id = ? AND status = ?";
        return queryForLong(sql, teamId, studentId, RequestStatus.PENDING) > 0;
    }

    @Override
    public List<JoinRequest> findPendingByTeam(Long teamId) {
        String sql = "SELECT " + COLUMNS + " FROM join_requests r WHERE r.team_id = ? AND r.status = ? ORDER BY r.created_at";
        return queryForList(sql, teamId, RequestStatus.PENDING);
    }

    @Override
    public List<JoinRequest> findPendingByStudentInHackathon(Long studentId, Long hackathonId) {
        String sql = "SELECT " + COLUMNS + " FROM join_requests r "
                + "WHERE r.student_id = ? AND r.hackathon_id = ? AND r.status = ?";
        return queryForList(sql, studentId, hackathonId, RequestStatus.PENDING);
    }

    @Override
    public List<JoinRequest> findByStudent(Long studentId) {
        return queryForList("SELECT " + COLUMNS + " FROM join_requests r WHERE r.student_id = ? ORDER BY r.created_at DESC",
                studentId);
    }

    @Override
    public List<JoinRequest> findByLeader(Long leaderId) {
        String sql = "SELECT " + COLUMNS + " FROM join_requests r JOIN teams t ON t.id = r.team_id "
                + "WHERE t.leader_id = ? ORDER BY r.created_at DESC";
        return queryForList(sql, leaderId);
    }

    @Override
    public JoinRequest insert(JoinRequest request) {
        String sql = "INSERT INTO join_requests (team_id, hackathon_id, student_id, type, status) VALUES (?, ?, ?, ?, ?)";
        long id = insertAndGetId(sql, request.getTeamId(), request.getHackathonId(), request.getStudentId(),
                request.getType(), request.getStatus());
        request.setId(id);
        return request;
    }

    // Only the status of a request ever changes.
    @Override
    public boolean update(JoinRequest request) {
        return updateStatus(request.getId(), request.getStatus());
    }

    @Override
    public boolean updateStatus(Long requestId, RequestStatus status) {
        return executeUpdate("UPDATE join_requests SET status = ? WHERE id = ?", status, requestId) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM join_requests WHERE id = ?", id) == 1;
    }
}
