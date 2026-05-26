package com.lencode.paper.behavior.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.dto.BehaviorEventRequest;
import com.lencode.paper.behavior.service.BehaviorTrackingService;
import com.lencode.paper.behavior.vo.BehaviorAcceptedResponse;

@RestController
public class BehaviorEventController {

    private final AuthService authService;
    private final BehaviorTrackingService trackingService;

    public BehaviorEventController(AuthService authService, BehaviorTrackingService trackingService) {
        this.authService = authService;
        this.trackingService = trackingService;
    }

    @PostMapping("/api/behavior-events")
    public ResponseEntity<BehaviorAcceptedResponse> record(@RequestBody BehaviorEventRequest request) {
        UserResponse user = authService.currentUser();
        BehaviorEventMessage message = trackingService.recordExplicit(user.getId(), request);
        return ResponseEntity.ok(new BehaviorAcceptedResponse(message.getEventId(), "ACCEPTED"));
    }
}
