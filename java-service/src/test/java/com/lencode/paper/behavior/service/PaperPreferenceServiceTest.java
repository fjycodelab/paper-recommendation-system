package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.entity.PaperFavorite;
import com.lencode.paper.behavior.entity.PaperRating;
import com.lencode.paper.behavior.mapper.PaperFavoriteMapper;
import com.lencode.paper.behavior.mapper.PaperRatingMapper;
import com.lencode.paper.behavior.vo.UserPaperStateResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;

class PaperPreferenceServiceTest {

    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperFavoriteMapper favoriteMapper = mock(PaperFavoriteMapper.class);
    private final PaperRatingMapper ratingMapper = mock(PaperRatingMapper.class);
    private final PaperPreferenceService service = new PaperPreferenceService(
            paperMapper,
            favoriteMapper,
            ratingMapper
    );

    @Test
    void favoritesActivePaper() {
        when(paperMapper.selectActiveById(9L)).thenReturn(paper(9L));
        when(favoriteMapper.selectByUserAndPaper(7L, 9L)).thenReturn(favorite("ACTIVE"));

        UserPaperStateResponse response = service.favorite(9L, user());

        verify(favoriteMapper).upsertActive(7L, 9L);
        assertThat(response.getPaperId()).isEqualTo(9L);
        assertThat(response.isFavorited()).isTrue();
    }

    @Test
    void cancelsFavoriteIdempotently() {
        when(paperMapper.selectActiveById(9L)).thenReturn(paper(9L));
        when(favoriteMapper.selectByUserAndPaper(7L, 9L)).thenReturn(favorite("CANCELLED"));

        UserPaperStateResponse response = service.cancelFavorite(9L, user());

        verify(favoriteMapper).cancel(7L, 9L);
        assertThat(response.isFavorited()).isFalse();
    }

    @Test
    void ratesPaperBetweenOneAndFive() {
        when(paperMapper.selectActiveById(9L)).thenReturn(paper(9L));
        when(ratingMapper.selectByUserAndPaper(7L, 9L)).thenReturn(rating(5));

        UserPaperStateResponse response = service.rate(9L, 5, user());

        verify(ratingMapper).upsertRating(7L, 9L, 5);
        assertThat(response.getRating()).isEqualTo(5);
    }

    @Test
    void rejectsInvalidRating() {
        when(paperMapper.selectActiveById(9L)).thenReturn(paper(9L));

        assertThatThrownBy(() -> service.rate(9L, 6, user()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("评分必须在 1 到 5 之间");
        verify(ratingMapper, never()).upsertRating(7L, 9L, 6);
    }

    @Test
    void rejectsMissingPaper() {
        when(paperMapper.selectActiveById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.favorite(404L, user()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("论文不存在");
        verify(favoriteMapper, never()).upsertActive(7L, 404L);
    }

    private static UserResponse user() {
        return new UserResponse(7L, "alice", "USER", "ACTIVE");
    }

    private static Paper paper(Long id) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setStatus("ACTIVE");
        return paper;
    }

    private static PaperFavorite favorite(String status) {
        PaperFavorite favorite = new PaperFavorite();
        favorite.setStatus(status);
        return favorite;
    }

    private static PaperRating rating(Integer value) {
        PaperRating rating = new PaperRating();
        rating.setRating(value);
        return rating;
    }
}
