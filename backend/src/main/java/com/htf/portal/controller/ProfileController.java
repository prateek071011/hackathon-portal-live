package com.htf.portal.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.PictureData;
import com.htf.portal.dto.ProfileForm;
import com.htf.portal.dto.ProfileView;
import com.htf.portal.security.AuthUser;
import com.htf.portal.service.ProfileService;

import jakarta.validation.Valid;

/**
 * The logged-in user's own profile (/api/profile) and any user's picture (/api/users/{id}/picture).
 * @AuthenticationPrincipal hands us the AuthUser that JwtAuthFilter read from the token.
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileView> myProfile(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(profileService.getProfile(me.id()));
    }

    @PutMapping("/profile")
    public ApiResponse<ProfileView> updateProfile(@AuthenticationPrincipal AuthUser me,
                                                  @Valid @RequestBody ProfileForm form) {
        return ApiResponse.ok("Profile saved", profileService.updateProfile(me.id(), form));
    }

    // multipart/form-data upload with one field called "file".
    @PostMapping("/profile/picture")
    public ApiResponse<ProfileView> uploadPicture(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Picture uploaded", profileService.uploadPicture(me.id(), file));
    }

    // Returns raw image bytes, not JSON, so the browser can show it as a picture.
    @GetMapping("/users/{id}/picture")
    public ResponseEntity<byte[]> picture(@PathVariable Long id) {
        PictureData picture = profileService.getPicture(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(picture.bytes());
    }
}
