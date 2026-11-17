package com.htf.portal.dto;

import java.time.LocalDateTime;

import com.htf.portal.model.Notification;

/**
 * One notification as the Notifications page shows it.
 */
public record NotificationView(Long id, String message, boolean read, LocalDateTime createdAt) {

    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
