package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.College;

/**
 * Plain-JDBC version of CollegeDao: each method is one SQL statement,
 * run through the helpers in AbstractJdbcDao.
 */
@Repository
public class JdbcCollegeDao extends AbstractJdbcDao<College> implements CollegeDao {

    private static final String COLUMNS = "id, name, created_at";

    public JdbcCollegeDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected College mapRow(ResultSet rs) throws SQLException {
        return new College(rs.getLong("id"), rs.getString("name"), getDateTime(rs, "created_at"));
    }

    @Override
    public Optional<College> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM colleges WHERE id = ?", id);
    }

    @Override
    public Optional<College> findByName(String name) {
        return queryForOne("SELECT " + COLUMNS + " FROM colleges WHERE name = ?", name);
    }

    @Override
    public List<College> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM colleges ORDER BY name");
    }

    @Override
    public College insert(College college) {
        long id = insertAndGetId("INSERT INTO colleges (name) VALUES (?)", college.getName());
        college.setId(id);
        return college;
    }

    @Override
    public boolean update(College college) {
        return executeUpdate("UPDATE colleges SET name = ? WHERE id = ?", college.getName(), college.getId()) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM colleges WHERE id = ?", id) == 1;
    }
}
