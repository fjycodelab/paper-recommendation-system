package com.lencode.paper.behavior.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.behavior.service.BehaviorStatsService;
import com.lencode.paper.behavior.vo.BehaviorStatsResponse;

@RestController
public class BehaviorStatsController {

    private final BehaviorStatsService statsService;

    public BehaviorStatsController(BehaviorStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/admin/behavior/stats")
    public ResponseEntity<BehaviorStatsResponse> globalStats() {
        return ResponseEntity.ok(statsService.getGlobalStats());
    }
}
