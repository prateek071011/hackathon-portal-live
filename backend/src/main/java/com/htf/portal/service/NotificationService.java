package com.htf.portal.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.htf.portal.dao.NotificationDao;
import com.htf.portal.dto.NotificationView;
import com.htf.portal.model.Notification;

/**
 * Stores short messages for users ("Ravi accepted your invite") and lists them. There is no live push and
 * no polling: the frontend reads them when the Notifications page opens and the unread count on page load.
 */
@Service
public class NotificationService {

    private final NotificationDao notificationDao;

    public NotificationService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    // Called inside other services' transactions, so a rolled-back action leaves no notification behind.
    public void notify(Long userId, String message) {
        notificationDao.insert(new Notification(userId, message));
    }

    public List<NotificationView> listFor(Long userId) {
        List<NotificationView> views = new ArrayList<>();
        for (Notification n : notificationDao.findByUser(userId)) {
            views.add(NotificationView.from(n));
        }
        return views;
    }

    public long unreadCount(Long userId) {
        return notificationDao.countUnread(userId);
    }

    public int markAllRead(Long userId) {
        return notificationDao.markAllRead(userId);
    }
}
