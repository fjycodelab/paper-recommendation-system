package com.lencode.paper.importer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.importer.dto.ArxivImportRequest;
import com.lencode.paper.importer.service.ArxivImportService;
import com.lencode.paper.importer.vo.ArxivImportResponse;

@RestController
public class ArxivImportController {

    private final ArxivImportService importService;
    private final AuthService authService;

    public ArxivImportController(ArxivImportService importService, AuthService authService) {
        this.importService = importService;
        this.authService = authService;
    }

    @PostMapping("/api/admin/papers/import/arxiv")
    public ResponseEntity<ArxivImportResponse> importArxiv(@RequestBody(required = false) ArxivImportRequest request) {
        UserResponse importer = authService.currentUser();
        return ResponseEntity.ok(importService.importPapers(request, importer));
    }
}
