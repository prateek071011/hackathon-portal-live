package com.htf.portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.htf.portal.exception.DatabaseException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.model.BaseEntity;

/**
 * Shared JDBC plumbing for every DAO: get a connection, fill in "?" parameters, run the SQL,
 * read generated keys, and turn SQLException into our unchecked DatabaseException.
 * Each child DAO only writes its SQL strings and mapRow(), which turns one row into a model object.
 */
public abstract class AbstractJdbcDao<T extends BaseEntity> {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private final DataSource dataSource;

    protected AbstractJdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Reads the current row of the ResultSet into a model object. Every DAO writes this by hand.
     * It is protected (not private) because this base class calls it from queryForList().
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /** Runs a SELECT that should return at most one row. */
    protected Optional<T> queryForOne(String sql, Object... params) {
        List<T> rows = queryForList(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    /** Runs a SELECT and maps every row with mapRow(). */
    protected List<T> queryForList(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        // Statement and ResultSet are closed by try-with-resources. The Connection is NOT:
        // inside a @Transactional method Spring shares one connection across several DAO calls,
        // so we hand it back with releaseConnection() and let Spring decide when to close it.
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw translate(e, sql);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** Runs a SELECT that returns a single number, e.g. SELECT COUNT(*) ... */
    protected long queryForLong(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw translate(e, sql);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /**
     * Runs a "SELECT some_id, COUNT(*) ... GROUP BY some_id" query and returns the answer as a HashMap:
     * id -> count. Ids that have no rows are simply missing from the map.
     */
    protected Map<Long, Integer> queryForCounts(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, Integer> counts = new HashMap<>();
                while (rs.next()) {
                    counts.put(rs.getLong(1), rs.getInt(2));
                }
                return counts;
            }
        } catch (SQLException e) {
            throw translate(e, sql);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** Runs an INSERT and returns the AUTO_INCREMENT id MySQL generated for the new row. */
    protected long insertAndGetId(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new DatabaseException("Insert did not return a generated id", null);
            }
        } catch (SQLException e) {
            throw translate(e, sql);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** Runs an UPDATE or DELETE and returns how many rows it changed. */
    protected int executeUpdate(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(e, sql);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** Fills the "?" placeholders in order. Values are never glued into the SQL text, so no SQL injection. */
    protected void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof Enum<?> enumValue) {
                // Our enums are stored as their name, e.g. TeamStatus.OPEN -> 'OPEN'.
                ps.setString(i + 1, enumValue.name());
            } else {
                // JDBC parameters start at 1, not 0. LocalDate / LocalDateTime / null all work with setObject.
                ps.setObject(i + 1, value);
            }
        }
    }

    // --- small helpers for mapRow(): read nullable columns without surprises ---

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    // rs.getLong() returns 0 for SQL NULL, so we check wasNull() to give back a real null.
    protected static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    private DatabaseException translate(SQLException e, String sql) {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            return new DuplicateKeyException(e.getMessage(), e);
        }
        return new DatabaseException("Database error while running: " + sql, e);
    }
}
