package com.lencode.paper.download.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.download.service.PaperDownloadService;
import com.lencode.paper.download.vo.DownloadAttemptResponse;

@RestController
public class DownloadController {

    private final PaperDownloadService downloadService;
    private final AuthService authService;

    public DownloadController(PaperDownloadService downloadService, AuthService authService) {
        this.downloadService = downloadService;
        this.authService = authService;
    }

    @PostMapping("/api/papers/{id}/download-attempt")
    public ResponseEntity<DownloadAttemptResponse> attempt(@PathVariable Long id) {
        UserResponse requester = authService.currentUser();
        return ResponseEntity.ok(downloadService.attempt(id, requester));
    }
}
