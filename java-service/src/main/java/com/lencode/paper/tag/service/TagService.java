package com.lencode.paper.tag.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.tag.dto.CreateTagRequest;
import com.lencode.paper.tag.dto.UpdateTagStatusRequest;
import com.lencode.paper.tag.entity.ResearchTag;
import com.lencode.paper.tag.mapper.ResearchTagMapper;
import com.lencode.paper.tag.vo.TagResponse;
import com.lencode.paper.tag.vo.TagTreeNodeResponse;

@Service
public class TagService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DISABLED_STATUS = "DISABLED";
    private static final int FIRST_LEVEL = 1;
    private static final int SECOND_LEVEL = 2;
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final int MAX_NAME_LENGTH = 100;

    private final ResearchTagMapper tagMapper;

    public TagService(ResearchTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<TagTreeNodeResponse> listActiveTree() {
        List<ResearchTag> tags = tagMapper.selectList(new QueryWrapper<ResearchTag>()
                .eq("status", ACTIVE_STATUS)
                .orderByAsc("level", "parent_id", "sort_order", "id"));
        Map<Long, TagTreeNodeResponse> parents = new LinkedHashMap<>();
        List<TagTreeNodeResponse> roots = new ArrayList<>();

        for (ResearchTag tag : tags) {
            if (tag.getLevel() == FIRST_LEVEL) {
                TagTreeNodeResponse parent = TagTreeNodeResponse.from(tag);
                parents.put(tag.getId(), parent);
                roots.add(parent);
            }
        }
        for (ResearchTag tag : tags) {
            if (tag.getLevel() == SECOND_LEVEL && parents.containsKey(tag.getParentId())) {
                parents.get(tag.getParentId()).getChildren().add(TagTreeNodeResponse.from(tag));
            }
        }
        return roots;
    }

    public TagResponse create(CreateTagRequest request) {
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }
        String name = normalizeName(request.getName());
        validateName(name);

        Long parentId = request.getParentId();
        int level = FIRST_LEVEL;
        if (parentId != null) {
            ResearchTag parent = tagMapper.selectById(parentId);
            if (parent == null) {
                throw new BadRequestException("父标签不存在");
            }
            if (parent.getLevel() != FIRST_LEVEL) {
                throw new BadRequestException("只能在一级标签下创建二级标签");
            }
            if (!ACTIVE_STATUS.equals(parent.getStatus())) {
                throw new BadRequestException("父标签已禁用");
            }
            level = SECOND_LEVEL;
        }

        if (existsByParentAndName(parentId, name)) {
            throw new BadRequestException("标签名称已存在");
        }

        ResearchTag tag = new ResearchTag();
        tag.setCode(newCode());
        tag.setParentId(parentId);
        tag.setName(name);
        tag.setLevel(level);
        tag.setStatus(ACTIVE_STATUS);
        tag.setSortOrder(request.getSortOrder() == null ? DEFAULT_SORT_ORDER : request.getSortOrder());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException ex) {
            throw new BadRequestException("标签名称已存在");
        }
        ResearchTag inserted = tagMapper.selectById(tag.getId());
        return TagResponse.from(inserted == null ? tag : inserted);
    }

    public TagResponse updateStatus(Long id, UpdateTagStatusRequest request) {
        if (id == null) {
            throw new BadRequestException("标签 id 不能为空");
        }
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }
        String status = normalizeStatus(request.getStatus());
        if (!ACTIVE_STATUS.equals(status) && !DISABLED_STATUS.equals(status)) {
            throw new BadRequestException("标签状态不合法");
        }

        ResearchTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new NotFoundException("标签不存在");
        }
        tag.setStatus(status);
        tagMapper.updateById(tag);
        ResearchTag updated = tagMapper.selectById(id);
        return TagResponse.from(updated == null ? tag : updated);
    }

    private boolean existsByParentAndName(Long parentId, String name) {
        QueryWrapper<ResearchTag> wrapper = new QueryWrapper<ResearchTag>().eq("name", name);
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        Long count = tagMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private static void validateName(String name) {
        if (!hasText(name)) {
            throw new BadRequestException("标签名称不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("标签名称过长");
        }
    }

    private static String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeStatus(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String newCode() {
        return "tag-" + UUID.randomUUID().toString().replace("-", "");
    }
}

