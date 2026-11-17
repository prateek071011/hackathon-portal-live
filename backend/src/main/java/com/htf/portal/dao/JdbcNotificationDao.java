package com.htf.portal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.htf.portal.model.Notification;

/**
 * Plain-JDBC version of NotificationDao.
 */
@Repository
public class JdbcNotificationDao extends AbstractJdbcDao<Notification> implements NotificationDao {

    private static final String COLUMNS = "id, user_id, message, is_read, created_at";

    public JdbcNotificationDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification(rs.getLong("user_id"), rs.getString("message"));
        n.setId(rs.getLong("id"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(getDateTime(rs, "created_at"));
        return n;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return queryForOne("SELECT " + COLUMNS + " FROM notifications WHERE id = ?", id);
    }

    @Override
    public List<Notification> findAll() {
        return queryForList("SELECT " + COLUMNS + " FROM notifications ORDER BY created_at DESC");
    }

    @Override
    public List<Notification> findByUser(Long userId) {
        return queryForList("SELECT " + COLUMNS + " FROM notifications WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                userId);
    }

    @Override
    public long countUnread(Long userId) {
        return queryForLong("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE", userId);
    }

    @Override
    public Notification insert(Notification n) {
        long id = insertAndGetId("INSERT INTO notifications (user_id, message) VALUES (?, ?)", n.getUserId(), n.getMessage());
        n.setId(id);
        return n;
    }

    @Override
    public boolean update(Notification n) {
        return executeUpdate("UPDATE notifications SET is_read = ? WHERE id = ?", n.isRead(), n.getId()) == 1;
    }

    @Override
    public int markAllRead(Long userId) {
        return executeUpdate("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE", userId);
    }

    @Override
    public boolean deleteById(Long id) {
        return executeUpdate("DELETE FROM notifications WHERE id = ?", id) == 1;
    }
}
