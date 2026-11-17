package com.htf.portal.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.NotificationView;
import com.htf.portal.security.AuthUser;
import com.htf.portal.service.NotificationService;

/**
 * The logged-in user's own notifications. No WebSocket and no polling: the page asks when it opens.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(notificationService.listFor(me.id()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(notificationService.unreadCount(me.id()));
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok("Marked as read", notificationService.markAllRead(me.id()));
    }
}
