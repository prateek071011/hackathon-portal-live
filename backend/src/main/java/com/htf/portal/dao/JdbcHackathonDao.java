package com.htf.portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.htf.portal.exception.DatabaseException;
import com.htf.portal.model.Hackathon;

/**
 * Plain-JDBC version of HackathonDao. insertAll() shows a MANUAL transaction
 * (setAutoCommit(false) + batch + commit/rollback), used by the CSV import.
 */
@Repository
public class JdbcHackathonDao extends AbstractJdbcDao<Hackathon> implements HackathonDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcHackathonDao.class);

    private static final String COLUMNS = "id, title, organizer, description, start_date, end_date, "
            + "registration_deadline, min_team_size, max_team_size, created_at";
    private static final String INSERT_SQL = "INSERT INTO hackathons (title, organizer, description, start_date, "
            + "end_date, registration_deadline, min_team_size, max_team_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public JdbcHackathonDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected Hackathon mapRow(ResultSet rs) throws SQLException {
        Hackathon h = new Hackathon();
        h.setId(rs.getLong("id"));
        h.setTitle(rs.getString("title"));
        h.setOrganizer(rs.getString("organizer"));
        h.setDescription(rs.getString("description"));
        h.setStartDate(getDate(rs, "start_date"));
        h.setEndDate(getDate(rs, "end_date"));
        h.setRegistrationDeadline(getDate(rs, "registration_deadline"));
        h.setMinTeamSize(rs.getInt("min_team_size"));
        h.setMaxTeamSize(rs.getInt("max_team_size"));
        h.setCreatedAt(getDateTime(rs, "created_at"));
        return h;
    }

    @Override
    public Optional<Hackathon> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM hackathons WHERE id = ?", id);
    }

    @Override
    public List<Hackathon> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM hackathons ORDER BY registration_deadline, title");
    }

    @Override
    public Hackathon insert(Hackathon h) {
        long id = insertAndGetId(INSERT_SQL, insertValues(h));
        h.setId(id);
        return h;
    }

    @Override
    public boolean update(Hackathon h) {
        String sql = "UPDATE hackathons SET title = ?, organizer = ?, description = ?, start_date = ?, end_date = ?, "
                + "registration_deadline = ?, min_team_size = ?, max_team_size = ? WHERE id = ?";
        int rows = executeUpdate(sql, h.getTitle(), h.getOrganizer(), h.getDescription(), h.getStartDate(),
                h.getEndDate(), h.getRegistrationDeadline(), h.getMinTeamSize(), h.getMaxTeamSize(), h.getId());
        return rows == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM hackathons WHERE id = ?", id) == 1;
    }

    @Override
    public long countTeams(Long hackathonId) {
        return queryForLong("SELECT COUNT(*) FROM teams WHERE hackathon_id = ?", hackathonId);
    }

    @Override
    public Map<Long, Integer> countTeamsPerHackathon() {
        return queryForCounts("SELECT hackathon_id, COUNT(*) FROM teams GROUP BY hackathon_id");
    }

    /**
     * Manual transaction, written out by hand on purpose (compare with @Transactional in the services).
     * We take our OWN connection straight from the pool instead of DataSourceUtils, because here our code,
     * not Spring, decides when to commit. All rows go to MySQL in one batch.
     * Success -> commit(): every row is saved. Any SQLException -> rollback(): no row is saved.
     */
    @Override
    public int insertAll(List<Hackathon> hackathons) {
        try (Connection con = getDataSource().getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
                for (Hackathon h : hackathons) {
                    setParams(ps, insertValues(h));
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                con.commit();
                return results.length;
            } catch (SQLException e) {
                con.rollback();
                log.warn("CSV batch insert failed, rolled back {} rows", hackathons.size());
                throw e;
            } finally {
                // The connection goes back to the pool and will be reused: leave it in its normal mode.
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Batch insert of hackathons failed and was rolled back", e);
        }
    }

    private static Object[] insertValues(Hackathon h) {
        return new Object[] { h.getTitle(), h.getOrganizer(), h.getDescription(), h.getStartDate(), h.getEndDate(),
                h.getRegistrationDeadline(), h.getMinTeamSize(), h.getMaxTeamSize() };
    }
}
