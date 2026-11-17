package com.htf.portal.dao;

import java.util.List;

import com.htf.portal.model.Notification;

/**
 * Database access for the notifications table.
 * The basic find/insert/update/delete come from GenericDao. Extra queries are listed here.
 */
public interface NotificationDao extends GenericDao<Notification, Long> {

    List<Notification> findByUser(Long userId);

    long countUnread(Long userId);

    int markAllRead(Long userId);
}
