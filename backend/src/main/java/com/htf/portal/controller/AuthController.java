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
import com.htf.portal.dto.LoginForm;
import com.htf.portal.dto.LoginResult;
import com.htf.portal.dto.SignupForm;
import com.htf.portal.model.College;
import com.htf.portal.service.AuthService;
import com.htf.portal.service.CollegeService;

import jakarta.validation.Valid;

/**
 * Public endpoints under /api/auth: signup, login, and the college list the signup form needs
 * (it has to work before anyone is logged in, so it lives here too).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CollegeService collegeService;

    public AuthController(AuthService authService, CollegeService collegeService) {
        this.authService = authService;
        this.collegeService = collegeService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResult> signup(@Valid @RequestBody SignupForm form) {
        LoginResult result = authService.signup(form);
        return ApiResponse.ok("Welcome! Your user code is " + result.user().userCode(), result);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginForm form) {
        return ApiResponse.ok("Logged in", authService.login(form));
    }

    @GetMapping("/colleges")
    public ApiResponse<List<College>> colleges() {
        return ApiResponse.ok(collegeService.listAll());
    }
}
