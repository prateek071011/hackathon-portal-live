package com.htf.portal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.MyRequestsView;
import com.htf.portal.security.AuthUser;
import com.htf.portal.service.RequestService;

/**
 * My requests, and answering them. Actions are POST sub-resources: /api/requests/{id}/accept etc.
 */
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/mine")
    public ApiResponse<MyRequestsView> mine(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(requestService.myRequests(me.id()));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(requestService.accept(me.id(), id), null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(requestService.reject(me.id(), id), null);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(requestService.cancel(me.id(), id), null);
    }
}
