package com.lencode.paper.paper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.entity.PaperRating;
import com.lencode.paper.behavior.mapper.PaperFavoriteMapper;
import com.lencode.paper.behavior.mapper.PaperRatingMapper;
import com.lencode.paper.behavior.service.RecentViewService;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.paper.dto.CreatePaperRequest;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;
import com.lencode.paper.paper.mapper.PaperTagMapper;
import com.lencode.paper.paper.vo.PaperPageResponse;
import com.lencode.paper.paper.vo.PaperResponse;
import com.lencode.paper.tag.entity.ResearchTag;
import com.lencode.paper.tag.mapper.ResearchTagMapper;
import com.lencode.paper.tag.vo.TagResponse;

class PaperServiceTest {

    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperTagMapper paperTagMapper = mock(PaperTagMapper.class);
    private final ResearchTagMapper tagMapper = mock(ResearchTagMapper.class);
    private final PaperFavoriteMapper favoriteMapper = mock(PaperFavoriteMapper.class);
    private final PaperRatingMapper ratingMapper = mock(PaperRatingMapper.class);
    private final RecentViewService recentViewService = mock(RecentViewService.class);
    private final PaperService paperService = new PaperService(
            paperMapper,
            paperTagMapper,
            tagMapper,
            favoriteMapper,
            ratingMapper,
            recentViewService
    );

    @Test
    void createsPaperWithEmptyBusinessFields() {
        CreatePaperRequest request = new CreatePaperRequest();
        UserResponse submitter = new UserResponse(7L, "alice", "USER", "ACTIVE");
        stubPaperInsertId(1L);
        when(paperMapper.selectActiveById(1L)).thenReturn(paper(1L, null, "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(1L)).thenReturn(Collections.emptyList());

        PaperResponse response = paperService.create(request, submitter);

        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isNull();
        assertThat(response.getSubmittedBy()).isEqualTo(7L);
        assertThat(paperCaptor.getValue().getSubmittedBy()).isEqualTo(7L);
        verify(paperTagMapper, never()).insertTag(any(Long.class), any(Long.class));
    }

    @Test
    void trimsFieldsAndParsesPublishedAt() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setTitle("  Paper Title  ");
        request.setPublishedAt("2026-05-25T12:30:00");
        UserResponse submitter = new UserResponse(7L, "alice", "USER", "ACTIVE");
        stubPaperInsertId(1L);
        when(paperMapper.selectActiveById(1L)).thenReturn(paper(1L, "Paper Title", "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(1L)).thenReturn(Collections.emptyList());

        PaperResponse response = paperService.create(request, submitter);

        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        assertThat(paperCaptor.getValue().getPublishedAt()).isEqualTo(LocalDateTime.parse("2026-05-25T12:30:00"));
        assertThat(response.getTitle()).isEqualTo("Paper Title");
    }

    @Test
    void createsPaperWithSecondLevelTags() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setTitle("Tagged Paper");
        request.setTagIds(Arrays.asList(2L, 2L, 3L, null));
        UserResponse submitter = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(tagMapper.selectById(2L)).thenReturn(tag(2L, 1L, "推荐系统", 2, "ACTIVE"));
        when(tagMapper.selectById(3L)).thenReturn(tag(3L, 1L, "语义检索", 2, "ACTIVE"));
        stubPaperInsertId(10L);
        when(paperMapper.selectActiveById(10L)).thenReturn(paper(10L, "Tagged Paper", "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(10L)).thenReturn(Arrays.asList(
                tag(2L, 1L, "推荐系统", 2, "ACTIVE"),
                tag(3L, 1L, "语义检索", 2, "ACTIVE")
        ));

        PaperResponse response = paperService.create(request, submitter);

        verify(paperTagMapper).insertTag(10L, 2L);
        verify(paperTagMapper).insertTag(10L, 3L);
        assertThat(response.getTags()).extracting(TagResponse::getName).containsExactly("推荐系统", "语义检索");
    }

    @Test
    void rejectsFirstLevelTagBinding() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setTagIds(Arrays.asList(1L));
        when(tagMapper.selectById(1L)).thenReturn(tag(1L, null, "人工智能", 1, "ACTIVE"));

        assertThatThrownBy(() -> paperService.create(request, new UserResponse(7L, "alice", "USER", "ACTIVE")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("标签不存在或已禁用");
    }

    @Test
    void rejectsDisabledTagBinding() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setTagIds(Arrays.asList(2L));
        when(tagMapper.selectById(2L)).thenReturn(tag(2L, 1L, "推荐系统", 2, "DISABLED"));

        assertThatThrownBy(() -> paperService.create(request, new UserResponse(7L, "alice", "USER", "ACTIVE")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("标签不存在或已禁用");
    }

    @Test
    void rejectsInvalidYear() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setPublishYear(4000);

        assertThatThrownBy(() -> paperService.create(request, new UserResponse(7L, "alice", "USER", "ACTIVE")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("年份不合法");
        verify(paperMapper, never()).insert(any(Paper.class));
    }

    @Test
    void rejectsNegativeCitationCount() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setCitationCount(-1);

        assertThatThrownBy(() -> paperService.create(request, new UserResponse(7L, "alice", "USER", "ACTIVE")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("引用量不能为负数");
    }

    @Test
    void mapsDuplicatePaperToBadRequest() {
        CreatePaperRequest request = new CreatePaperRequest();
        when(paperMapper.insert(any(Paper.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> paperService.create(request, new UserResponse(7L, "alice", "USER", "ACTIVE")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("论文已存在");
    }

    @Test
    void returnsPagedActivePapers() {
        when(paperMapper.selectActivePage(isNull(), eq(5), eq(5)))
                .thenReturn(Arrays.asList(paper(2L, "Second", "ACTIVE", 1L)));
        when(paperMapper.countActive(isNull())).thenReturn(12L);
        when(paperTagMapper.selectTagsByPaperId(2L)).thenReturn(Collections.emptyList());

        PaperPageResponse response = paperService.list(2, 5);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(5);
        assertThat(response.getTotal()).isEqualTo(12L);
        assertThat(response.getItems()).extracting(PaperResponse::getTitle).containsExactly("Second");
    }

    @Test
    void returnsPagedActivePapersWithUserState() {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(paperMapper.selectActivePage(isNull(), eq(5), eq(5)))
                .thenReturn(Arrays.asList(paper(2L, "Second", "ACTIVE", 1L)));
        when(paperMapper.countActive(isNull())).thenReturn(12L);
        when(paperTagMapper.selectTagsByPaperId(2L)).thenReturn(Collections.emptyList());
        when(favoriteMapper.selectActivePaperIdsForUserAndPaperIds(7L, Arrays.asList(2L)))
                .thenReturn(Arrays.asList(2L));
        when(ratingMapper.selectByUserAndPaperIds(7L, Arrays.asList(2L)))
                .thenReturn(Arrays.asList(rating(2L, 4)));

        PaperPageResponse response = paperService.list(2, 5, null, user);

        assertThat(response.getItems().get(0).isFavorited()).isTrue();
        assertThat(response.getItems().get(0).getRating()).isEqualTo(4);
    }

    @Test
    void returnsSearchResultsWithTags() {
        PaperSearchRequest search = new PaperSearchRequest(" paper ", " Alice ", 2026, " arXiv ", 2L, " embedding ");
        when(paperMapper.selectActivePage(any(PaperSearchRequest.class), eq(10), eq(0)))
                .thenReturn(Arrays.asList(paper(3L, "Paper", "ACTIVE", 1L)));
        when(paperMapper.countActive(any(PaperSearchRequest.class))).thenReturn(1L);
        when(paperTagMapper.selectTagsByPaperId(3L))
                .thenReturn(Arrays.asList(tag(2L, 1L, "推荐系统", 2, "ACTIVE")));

        PaperPageResponse response = paperService.list(1, 10, search);

        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getTags()).extracting(TagResponse::getName).containsExactly("推荐系统");
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> paperService.list(0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("页码必须大于等于 1");

        assertThatThrownBy(() -> paperService.list(1, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("每页数量必须在 1 到 100 之间");
    }

    @Test
    void getsActivePaperById() {
        when(paperMapper.selectActiveById(1L)).thenReturn(paper(1L, "Paper", "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(1L))
                .thenReturn(Arrays.asList(tag(2L, 1L, "推荐系统", 2, "ACTIVE")));

        PaperResponse response = paperService.get(1L);

        assertThat(response.getTitle()).isEqualTo("Paper");
        assertThat(response.getTags()).extracting(TagResponse::getName).containsExactly("推荐系统");
    }

    @Test
    void getsActivePaperByIdWithUserState() {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(paperMapper.selectActiveById(1L)).thenReturn(paper(1L, "Paper", "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(1L)).thenReturn(Collections.emptyList());
        when(favoriteMapper.selectActivePaperIdsForUserAndPaperIds(7L, Arrays.asList(1L)))
                .thenReturn(Arrays.asList(1L));
        when(ratingMapper.selectByUserAndPaperIds(7L, Arrays.asList(1L)))
                .thenReturn(Arrays.asList(rating(1L, 5)));

        PaperResponse response = paperService.get(1L, user);

        assertThat(response.isFavorited()).isTrue();
        assertThat(response.getRating()).isEqualTo(5);
    }

    @Test
    void returnsPagedFavoritePapersForUser() {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(favoriteMapper.selectActivePaperIdsByUser(7L, 10, 0)).thenReturn(Arrays.asList(3L, 2L));
        when(favoriteMapper.countActiveByUser(7L)).thenReturn(2L);
        when(paperMapper.selectActiveByIds(Arrays.asList(3L, 2L))).thenReturn(Arrays.asList(
                paper(2L, "Second", "ACTIVE", 1L),
                paper(3L, "Third", "ACTIVE", 1L)
        ));
        when(paperTagMapper.selectTagsByPaperId(2L)).thenReturn(Collections.emptyList());
        when(paperTagMapper.selectTagsByPaperId(3L)).thenReturn(Collections.emptyList());
        when(favoriteMapper.selectActivePaperIdsForUserAndPaperIds(7L, Arrays.asList(3L, 2L)))
                .thenReturn(Arrays.asList(3L, 2L));
        when(ratingMapper.selectByUserAndPaperIds(7L, Arrays.asList(3L, 2L)))
                .thenReturn(Arrays.asList(rating(3L, 5)));

        PaperPageResponse response = paperService.listFavorites(1, 10, user);

        assertThat(response.getTotal()).isEqualTo(2L);
        assertThat(response.getItems()).extracting(PaperResponse::getId).containsExactly(3L, 2L);
        assertThat(response.getItems()).allMatch(PaperResponse::isFavorited);
        assertThat(response.getItems().get(0).getRating()).isEqualTo(5);
        assertThat(response.getItems().get(1).getRating()).isNull();
    }

    @Test
    void returnsRecentViewedPapersForUser() {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(recentViewService.listRecentPaperIds(7L, 1, 10)).thenReturn(Arrays.asList(5L, 2L));
        when(recentViewService.countRecentViews(7L)).thenReturn(2L);
        when(paperMapper.selectActiveByIds(Arrays.asList(5L, 2L))).thenReturn(Arrays.asList(
                paper(2L, "Second", "ACTIVE", 1L),
                paper(5L, "Fifth", "ACTIVE", 1L)
        ));
        when(paperTagMapper.selectTagsByPaperId(2L)).thenReturn(Collections.emptyList());
        when(paperTagMapper.selectTagsByPaperId(5L)).thenReturn(Collections.emptyList());
        when(favoriteMapper.selectActivePaperIdsForUserAndPaperIds(7L, Arrays.asList(5L, 2L)))
                .thenReturn(Arrays.asList(2L));
        when(ratingMapper.selectByUserAndPaperIds(7L, Arrays.asList(5L, 2L)))
                .thenReturn(Arrays.asList(rating(5L, 4)));

        PaperPageResponse response = paperService.listRecentViews(1, 10, user);

        assertThat(response.getTotal()).isEqualTo(2L);
        assertThat(response.getItems()).extracting(PaperResponse::getId).containsExactly(5L, 2L);
        assertThat(response.getItems().get(0).getRating()).isEqualTo(4);
        assertThat(response.getItems().get(1).isFavorited()).isTrue();
    }

    @Test
    void hidesMissingOrDeletedPaper() {
        when(paperMapper.selectActiveById(9L)).thenReturn(null);

        assertThatThrownBy(() -> paperService.get(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("论文不存在");
    }

    @Test
    void updatesPaperAndReplacesTags() {
        CreatePaperRequest request = new CreatePaperRequest();
        request.setTitle("  Updated Paper  ");
        request.setTagIds(Arrays.asList(2L, 3L));
        when(paperMapper.selectById(5L))
                .thenReturn(
                        paper(5L, "Old Paper", "ACTIVE", 7L),
                        paper(5L, "Updated Paper", "ACTIVE", 7L)
                );
        when(tagMapper.selectById(2L)).thenReturn(tag(2L, 1L, "推荐系统", 2, "ACTIVE"));
        when(tagMapper.selectById(3L)).thenReturn(tag(3L, 1L, "语义检索", 2, "ACTIVE"));
        when(paperTagMapper.selectTagsByPaperId(5L)).thenReturn(Arrays.asList(
                tag(2L, 1L, "推荐系统", 2, "ACTIVE"),
                tag(3L, 1L, "语义检索", 2, "ACTIVE")
        ));

        PaperResponse response = paperService.update(5L, request);

        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperMapper).updateById(paperCaptor.capture());
        verify(paperTagMapper).deleteByPaperId(5L);
        verify(paperTagMapper).insertTag(5L, 2L);
        verify(paperTagMapper).insertTag(5L, 3L);
        assertThat(paperCaptor.getValue().getTitle()).isEqualTo("Updated Paper");
        assertThat(response.getTags()).extracting(TagResponse::getName).containsExactly("推荐系统", "语义检索");
    }

    @Test
    void mapsMissingPaperUpdateToNotFound() {
        when(paperMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> paperService.update(404L, new CreatePaperRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("论文不存在");
    }

    @Test
    void softDeletesPaperAndHidesItFromActiveDetail() {
        when(paperMapper.selectById(9L))
                .thenReturn(
                        paper(9L, "Paper", "ACTIVE", 7L),
                        paper(9L, "Paper", "DELETED", 7L)
                );
        when(paperTagMapper.selectTagsByPaperId(9L)).thenReturn(Collections.emptyList());

        PaperResponse response = paperService.softDelete(9L);

        verify(paperMapper).softDeleteById(9L);
        assertThat(response.getStatus()).isEqualTo("DELETED");
    }

    @Test
    void restoresDeletedPaper() {
        when(paperMapper.selectById(9L)).thenReturn(paper(9L, "Paper", "DELETED", 7L));
        when(paperMapper.selectActiveById(9L)).thenReturn(paper(9L, "Paper", "ACTIVE", 7L));
        when(paperTagMapper.selectTagsByPaperId(9L)).thenReturn(Collections.emptyList());

        PaperResponse response = paperService.restore(9L);

        verify(paperMapper).restoreById(9L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void returnsPagedDeletedPapers() {
        when(paperMapper.selectDeletedPage(eq(5), eq(0)))
                .thenReturn(Arrays.asList(paper(8L, "Deleted", "DELETED", 1L)));
        when(paperMapper.countDeleted()).thenReturn(1L);
        when(paperTagMapper.selectTagsByPaperId(8L)).thenReturn(Collections.emptyList());

        PaperPageResponse response = paperService.listDeleted(1, 5);

        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getItems()).extracting(PaperResponse::getStatus).containsExactly("DELETED");
    }

    private void stubPaperInsertId(Long id) {
        when(paperMapper.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            paper.setId(id);
            return 1;
        });
    }

    private static Paper paper(Long id, String title, String status, Long submittedBy) {
        return new Paper(
                id,
                title,
                "Alice",
                "Abstract",
                2026,
                "manual",
                null,
                null,
                null,
                null,
                "keyword",
                0,
                null,
                status,
                submittedBy,
                null,
                LocalDateTime.parse("2026-05-25T00:00:00"),
                LocalDateTime.parse("2026-05-25T00:00:00")
        );
    }

    private static PaperRating rating(Long paperId, Integer value) {
        PaperRating rating = new PaperRating();
        rating.setUserId(7L);
        rating.setPaperId(paperId);
        rating.setRating(value);
        return rating;
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
                LocalDateTime.parse("2026-05-25T00:00:00"),
                LocalDateTime.parse("2026-05-25T00:00:00")
        );
    }
}
