package com.lencode.paper.paper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.service.BehaviorTrackingService;
import com.lencode.paper.paper.dto.CreatePaperRequest;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.service.PaperService;
import com.lencode.paper.paper.vo.PaperPageResponse;
import com.lencode.paper.paper.vo.PaperResponse;

@RestController
public class PaperController {

    private final PaperService paperService;
    private final AuthService authService;
    private final BehaviorTrackingService trackingService;

    public PaperController(
            PaperService paperService,
            AuthService authService,
            BehaviorTrackingService trackingService) {
        this.paperService = paperService;
        this.authService = authService;
        this.trackingService = trackingService;
    }

    @PostMapping("/api/papers")
    public ResponseEntity<PaperResponse> create(@RequestBody CreatePaperRequest request) {
        UserResponse submitter = authService.currentUser();
        return ResponseEntity.ok(paperService.create(request, submitter));
    }

    @GetMapping("/api/papers")
    public ResponseEntity<PaperPageResponse> list(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "abstractKeyword", required = false) String abstractKeyword) {
        UserResponse user = authService.currentUser();
        PaperSearchRequest search = new PaperSearchRequest(title, author, year, source, tagId, abstractKeyword);
        PaperPageResponse response = paperService.list(
                page,
                pageSize,
                search,
                user
        );
        trackingService.recordSearch(user.getId(), search);
        trackingService.recordTagFilter(user.getId(), tagId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/papers/{id}")
    public ResponseEntity<PaperResponse> get(@PathVariable Long id) {
        UserResponse user = authService.currentUser();
        PaperResponse response = paperService.get(id, user);
        trackingService.recordPaperDetailView(user.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/me/favorites")
    public ResponseEntity<PaperPageResponse> listFavorites(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        UserResponse user = authService.currentUser();
        return ResponseEntity.ok(paperService.listFavorites(page, pageSize, user));
    }

    @GetMapping("/api/me/recent-views")
    public ResponseEntity<PaperPageResponse> listRecentViews(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        UserResponse user = authService.currentUser();
        return ResponseEntity.ok(paperService.listRecentViews(page, pageSize, user));
    }

    @PutMapping("/api/admin/papers/{id}")
    public ResponseEntity<PaperResponse> update(
            @PathVariable Long id,
            @RequestBody CreatePaperRequest request) {
        return ResponseEntity.ok(paperService.update(id, request));
    }

    @DeleteMapping("/api/admin/papers/{id}")
    public ResponseEntity<PaperResponse> softDelete(@PathVariable Long id) {
        return ResponseEntity.ok(paperService.softDelete(id));
    }

    @PostMapping("/api/admin/papers/{id}/restore")
    public ResponseEntity<PaperResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(paperService.restore(id));
    }

    @GetMapping("/api/admin/papers/deleted")
    public ResponseEntity<PaperPageResponse> listDeleted(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ResponseEntity.ok(paperService.listDeleted(page, pageSize));
    }
}

