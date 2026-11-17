package com.htf.portal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.HackathonForm;
import com.htf.portal.dto.HackathonView;
import com.htf.portal.dto.ImportResult;
import com.htf.portal.service.HackathonImportService;
import com.htf.portal.service.HackathonService;

import jakarta.validation.Valid;

/**
 * Admin-only hackathon endpoints (/api/admin/** needs the ADMIN role, see SecurityConfig):
 * create, edit, delete and CSV import.
 */
@RestController
@RequestMapping("/api/admin/hackathons")
public class AdminHackathonController {

    private final HackathonService hackathonService;
    private final HackathonImportService importService;

    public AdminHackathonController(HackathonService hackathonService, HackathonImportService importService) {
        this.hackathonService = hackathonService;
        this.importService = importService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HackathonView> create(@Valid @RequestBody HackathonForm form) {
        return ApiResponse.ok("Hackathon created", hackathonService.create(form));
    }

    @PutMapping("/{id}")
    public ApiResponse<HackathonView> update(@PathVariable Long id, @Valid @RequestBody HackathonForm form) {
        return ApiResponse.ok("Hackathon saved", hackathonService.update(id, form));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hackathonService.delete(id);
        return ApiResponse.ok("Hackathon deleted", null);
    }

    // multipart/form-data upload with one field called "file".
    @PostMapping("/import")
    public ApiResponse<ImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        ImportResult result = importService.importCsv(file);
        String message = "Imported " + countText(result.importedCount(), "hackathon");
        if (!result.errors().isEmpty()) {
            message = message + ", skipped " + countText(result.errors().size(), "bad line");
        }
        return ApiResponse.ok(message, result);
    }

    // "1 hackathon", "3 hackathons"
    private static String countText(int count, String word) {
        if (count == 1) {
            return count + " " + word;
        }
        return count + " " + word + "s";
    }
}
