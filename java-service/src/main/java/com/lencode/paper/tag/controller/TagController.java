package com.lencode.paper.tag.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lencode.paper.tag.dto.CreateTagRequest;
import com.lencode.paper.tag.dto.UpdateTagStatusRequest;
import com.lencode.paper.tag.service.TagService;
import com.lencode.paper.tag.vo.TagResponse;
import com.lencode.paper.tag.vo.TagTreeNodeResponse;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/tags")
    public ResponseEntity<List<TagTreeNodeResponse>> listActiveTree() {
        return ResponseEntity.ok(tagService.listActiveTree());
    }

    @PostMapping("/api/admin/tags")
    public ResponseEntity<TagResponse> create(@RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(tagService.create(request));
    }

    @PatchMapping("/api/admin/tags/{id}/status")
    public ResponseEntity<TagResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateTagStatusRequest request) {
        return ResponseEntity.ok(tagService.updateStatus(id, request));
    }
}

