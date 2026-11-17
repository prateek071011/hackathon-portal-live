package com.htf.portal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.CollegeForm;
import com.htf.portal.dto.CollegeView;
import com.htf.portal.service.CollegeService;

import jakarta.validation.Valid;

/**
 * Admin-only college endpoints: list colleges with their reviewer, and create a college + reviewer login.
 */
@RestController
@RequestMapping("/api/admin/colleges")
public class AdminCollegeController {

    private final CollegeService collegeService;

    public AdminCollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public ApiResponse<List<CollegeView>> list() {
        return ApiResponse.ok(collegeService.listWithReviewers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CollegeView> create(@Valid @RequestBody CollegeForm form) {
        return ApiResponse.ok("College created", collegeService.create(form));
    }
}
