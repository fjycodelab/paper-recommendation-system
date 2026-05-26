package com.lencode.paper.tag.vo;

import com.lencode.paper.tag.entity.ResearchTag;

public class TagResponse {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final int level;
    private final String status;
    private final int sortOrder;

    public TagResponse(Long id, Long parentId, String name, int level, String status, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.level = level;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    public static TagResponse from(ResearchTag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getParentId(),
                tag.getName(),
                tag.getLevel(),
                tag.getStatus(),
                tag.getSortOrder()
        );
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

    public String getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}

