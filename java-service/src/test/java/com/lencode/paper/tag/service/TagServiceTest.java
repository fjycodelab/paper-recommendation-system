package com.lencode.paper.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.tag.dto.CreateTagRequest;
import com.lencode.paper.tag.dto.UpdateTagStatusRequest;
import com.lencode.paper.tag.entity.ResearchTag;
import com.lencode.paper.tag.mapper.ResearchTagMapper;
import com.lencode.paper.tag.vo.TagResponse;
import com.lencode.paper.tag.vo.TagTreeNodeResponse;

class TagServiceTest {

    private final ResearchTagMapper tagMapper = mock(ResearchTagMapper.class);
    private final TagService tagService = new TagService(tagMapper);

    @Test
    void buildsActiveTreeWithOnlyChildrenUnderActiveParents() {
        ResearchTag ai = tag(1L, null, "人工智能", 1, "ACTIVE");
        ResearchTag recommender = tag(2L, 1L, "推荐系统", 2, "ACTIVE");
        ResearchTag orphan = tag(3L, 99L, "孤儿标签", 2, "ACTIVE");
        when(tagMapper.selectList(any())).thenReturn(Arrays.asList(ai, recommender, orphan));

        assertThat(tagService.listActiveTree()).hasSize(1);
        assertThat(tagService.listActiveTree().get(0).getName()).isEqualTo("人工智能");
        assertThat(tagService.listActiveTree().get(0).getChildren())
                .extracting(TagTreeNodeResponse::getName)
                .containsExactly("推荐系统");
    }

    @Test
    void createsFirstLevelTag() {
        CreateTagRequest request = new CreateTagRequest();
        request.setName("  新方向  ");
        request.setSortOrder(7);
        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(tagMapper.insert(any(ResearchTag.class))).thenAnswer(invocation -> {
            ResearchTag tag = invocation.getArgument(0);
            tag.setId(10L);
            return 1;
        });
        when(tagMapper.selectById(10L)).thenReturn(tag(10L, null, "新方向", 1, "ACTIVE"));

        TagResponse response = tagService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("新方向");
        assertThat(response.getLevel()).isEqualTo(1);
    }

    @Test
    void createsSecondLevelTagUnderFirstLevelParent() {
        CreateTagRequest request = new CreateTagRequest();
        request.setParentId(1L);
        request.setName("语义检索");
        when(tagMapper.selectById(1L)).thenReturn(tag(1L, null, "人工智能", 1, "ACTIVE"));
        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(tagMapper.insert(any(ResearchTag.class))).thenAnswer(invocation -> {
            ResearchTag tag = invocation.getArgument(0);
            tag.setId(11L);
            return 1;
        });
        when(tagMapper.selectById(11L)).thenReturn(tag(11L, 1L, "语义检索", 2, "ACTIVE"));

        TagResponse response = tagService.create(request);

        assertThat(response.getParentId()).isEqualTo(1L);
        assertThat(response.getLevel()).isEqualTo(2);
    }

    @Test
    void rejectsSecondLevelParent() {
        CreateTagRequest request = new CreateTagRequest();
        request.setParentId(2L);
        request.setName("错误层级");
        when(tagMapper.selectById(2L)).thenReturn(tag(2L, 1L, "推荐系统", 2, "ACTIVE"));

        assertThatThrownBy(() -> tagService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("只能在一级标签下创建二级标签");
        verify(tagMapper, never()).insert(any(ResearchTag.class));
    }

    @Test
    void rejectsDuplicateSiblingName() {
        CreateTagRequest request = new CreateTagRequest();
        request.setParentId(1L);
        request.setName("推荐系统");
        when(tagMapper.selectById(1L)).thenReturn(tag(1L, null, "人工智能", 1, "ACTIVE"));
        when(tagMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> tagService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("标签名称已存在");
    }

    @Test
    void updatesStatus() {
        UpdateTagStatusRequest request = new UpdateTagStatusRequest();
        request.setStatus("disabled");
        when(tagMapper.selectById(10L))
                .thenReturn(
                        tag(10L, 1L, "语义检索", 2, "ACTIVE"),
                        tag(10L, 1L, "语义检索", 2, "DISABLED")
                );
        when(tagMapper.updateById(any(ResearchTag.class))).thenReturn(1);

        TagResponse response = tagService.updateStatus(10L, request);

        assertThat(response.getStatus()).isEqualTo("DISABLED");
    }

    @Test
    void rejectsInvalidStatus() {
        UpdateTagStatusRequest request = new UpdateTagStatusRequest();
        request.setStatus("ARCHIVED");

        assertThatThrownBy(() -> tagService.updateStatus(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("标签状态不合法");
    }

    @Test
    void mapsMissingTagToNotFound() {
        UpdateTagStatusRequest request = new UpdateTagStatusRequest();
        request.setStatus("ACTIVE");
        when(tagMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> tagService.updateStatus(404L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("标签不存在");
    }

    private static ResearchTag tag(Long id, Long parentId, String name, int level, String status) {
        return new ResearchTag(
                id,
                "tag-" + id,
                parentId,
                name,
                level,
                status,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
