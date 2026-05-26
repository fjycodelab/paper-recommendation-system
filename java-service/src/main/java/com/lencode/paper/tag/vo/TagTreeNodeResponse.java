package com.lencode.paper.tag.vo;

import java.util.ArrayList;
import java.util.List;

import com.lencode.paper.tag.entity.ResearchTag;

public class TagTreeNodeResponse {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final int level;
    private final List<TagTreeNodeResponse> children;

    public TagTreeNodeResponse(Long id, Long parentId, String name, int level) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.level = level;
        this.children = new ArrayList<>();
    }

    public static TagTreeNodeResponse from(ResearchTag tag) {
        return new TagTreeNodeResponse(tag.getId(), tag.getParentId(), tag.getName(), tag.getLevel());
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public List<TagTreeNodeResponse> getChildren() {
        return children;
    }
}

