package com.htf.portal.controller;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.RegistrationDetail;
import com.htf.portal.dto.RegistrationRow;
import com.htf.portal.dto.RejectForm;
import com.htf.portal.model.RegistrationStatus;
import com.htf.portal.security.AuthUser;
import com.htf.portal.service.RegistrationService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * College-only endpoints (/api/college/** needs the COLLEGE role, see SecurityConfig): the review queue,
 * approve / reject, and the CSV of registered teams. The service checks the registration belongs to MY college.
 */
@RestController
@RequestMapping("/api/college/registrations")
public class CollegeController {

    private final RegistrationService registrationService;

    public CollegeController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // ?status=PENDING (the queue, default) or APPROVED (registered teams) or REJECTED.
    @GetMapping
    public ApiResponse<List<RegistrationRow>> list(@AuthenticationPrincipal AuthUser me,
                                                   @RequestParam(defaultValue = "PENDING") RegistrationStatus status) {
        return ApiResponse.ok(registrationService.listForCollege(me.id(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<RegistrationDetail> detail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(registrationService.detailForCollege(me.id(), id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.ok(registrationService.approve(me.id(), id), null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                    @Valid @RequestBody RejectForm form) {
        return ApiResponse.ok(registrationService.reject(me.id(), id, form.reason()), null);
    }

    // Not JSON: a CSV file the browser saves. OutputStreamWriter turns our characters into UTF-8 bytes.
    @GetMapping("/export")
    public void exportCsv(@AuthenticationPrincipal AuthUser me, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"registered-teams.csv\"");
        registrationService.exportApprovedCsv(me.id(), new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
    }
}
