package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.Role;
import com.htf.portal.model.User;

/**
 * Plain-JDBC version of UserDao. mapRow() copies one users row into a User object, column by column.
 */
@Repository
public class JdbcUserDao extends AbstractJdbcDao<User> implements UserDao {

    private static final String COLUMNS = "id, name, email, password_hash, role, college_id, user_code, "
            + "roll_number, branch, study_year, skills, picture_file, created_at";

    public JdbcUserDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setCollegeId(getNullableLong(rs, "college_id"));
        user.setUserCode(rs.getString("user_code"));
        user.setRollNumber(rs.getString("roll_number"));
        user.setBranch(rs.getString("branch"));
        // getInt() would turn NULL into 0, so read it as an Integer object instead.
        user.setStudyYear(rs.getObject("study_year", Integer.class));
        user.setSkills(rs.getString("skills"));
        user.setPictureFile(rs.getString("picture_file"));
        user.setCreatedAt(getDateTime(rs, "created_at"));
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryForOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", email);
    }

    @Override
    public Optional<User> findByUserCode(String userCode) {
        return queryForOne("SELECT " + COLUMNS + " FROM users WHERE user_code = ?", userCode);
    }

    @Override
    public List<User> findByRole(Role role) {
        return queryForList("SELECT " + COLUMNS + " FROM users WHERE role = ? ORDER BY name", role);
    }

    @Override
    public List<User> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM users ORDER BY name");
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryForLong("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    @Override
    public boolean existsByUserCode(String userCode) {
        return queryForLong("SELECT COUNT(*) FROM users WHERE user_code = ?", userCode) > 0;
    }

    @Override
    public User insert(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role, college_id, user_code, "
                + "roll_number, branch, study_year, skills) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        long id = insertAndGetId(sql,
                user.getName(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.getCollegeId(),
                user.getUserCode(), user.getRollNumber(), user.getBranch(), user.getStudyYear(), user.getSkills());
        user.setId(id);
        return user;
    }

    // Updates the editable profile fields. Email, role, college, user code and password never change here.
    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET name = ?, roll_number = ?, branch = ?, study_year = ?, skills = ? WHERE id = ?";
        int rows = executeUpdate(sql,
                user.getName(), user.getRollNumber(), user.getBranch(), user.getStudyYear(), user.getSkills(),
                user.getId());
        return rows == 1;
    }

    @Override
    public boolean updatePicture(Long userId, String pictureFile) {
        return executeUpdate("UPDATE users SET picture_file = ? WHERE id = ?", pictureFile, userId) == 1;
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM users WHERE id = ?", id) == 1;
    }
}
