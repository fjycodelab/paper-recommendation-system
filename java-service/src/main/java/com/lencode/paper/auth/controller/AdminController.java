package com.lencode.paper.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.vo.AdminPingResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public ResponseEntity<AdminPingResponse> ping() {
        return ResponseEntity.ok(new AdminPingResponse(true));
    }
}

