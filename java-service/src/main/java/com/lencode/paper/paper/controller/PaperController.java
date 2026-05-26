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
import com.lencode.paper.paper.dto.CreatePaperRequest;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.service.PaperService;
import com.lencode.paper.paper.vo.PaperPageResponse;
import com.lencode.paper.paper.vo.PaperResponse;

@RestController
public class PaperController {

    private final PaperService paperService;
    private final AuthService authService;

    public PaperController(PaperService paperService, AuthService authService) {
        this.paperService = paperService;
        this.authService = authService;
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
        return ResponseEntity.ok(paperService.list(
                page,
                pageSize,
                new PaperSearchRequest(title, author, year, source, tagId, abstractKeyword)
        ));
    }

    @GetMapping("/api/papers/{id}")
    public ResponseEntity<PaperResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(paperService.get(id));
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

