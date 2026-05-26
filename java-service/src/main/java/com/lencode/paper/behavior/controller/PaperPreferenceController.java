package com.lencode.paper.behavior.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.dto.RatePaperRequest;
import com.lencode.paper.behavior.service.PaperPreferenceService;
import com.lencode.paper.behavior.vo.UserPaperStateResponse;

@RestController
public class PaperPreferenceController {

    private final PaperPreferenceService preferenceService;
    private final AuthService authService;

    public PaperPreferenceController(PaperPreferenceService preferenceService, AuthService authService) {
        this.preferenceService = preferenceService;
        this.authService = authService;
    }

    @PostMapping("/api/papers/{id}/favorite")
    public ResponseEntity<UserPaperStateResponse> favorite(@PathVariable Long id) {
        UserResponse user = authService.currentUser();
        return ResponseEntity.ok(preferenceService.favorite(id, user));
    }

    @DeleteMapping("/api/papers/{id}/favorite")
    public ResponseEntity<UserPaperStateResponse> cancelFavorite(@PathVariable Long id) {
        UserResponse user = authService.currentUser();
        return ResponseEntity.ok(preferenceService.cancelFavorite(id, user));
    }

    @PutMapping("/api/papers/{id}/rating")
    public ResponseEntity<UserPaperStateResponse> rate(
            @PathVariable Long id,
            @RequestBody RatePaperRequest request) {
        UserResponse user = authService.currentUser();
        Integer rating = request == null ? null : request.getRating();
        return ResponseEntity.ok(preferenceService.rate(id, rating, user));
    }
}
