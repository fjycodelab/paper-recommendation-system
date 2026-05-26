package com.lencode.paper.behavior.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class PaperPreferenceService {

    private static final String FAVORITE_ACTIVE = "ACTIVE";

    private final PaperMapper paperMapper;
    private final PaperFavoriteMapper favoriteMapper;
    private final PaperRatingMapper ratingMapper;

    public PaperPreferenceService(
            PaperMapper paperMapper,
            PaperFavoriteMapper favoriteMapper,
            PaperRatingMapper ratingMapper) {
        this.paperMapper = paperMapper;
        this.favoriteMapper = favoriteMapper;
        this.ratingMapper = ratingMapper;
    }

    @Transactional
    public UserPaperStateResponse favorite(Long paperId, UserResponse user) {
        Long userId = requireUserId(user);
        ensureActivePaper(paperId);

        favoriteMapper.upsertActive(userId, paperId);
        return state(paperId, userId);
    }

    @Transactional
    public UserPaperStateResponse cancelFavorite(Long paperId, UserResponse user) {
        Long userId = requireUserId(user);
        ensureActivePaper(paperId);

        favoriteMapper.cancel(userId, paperId);
        return state(paperId, userId);
    }

    @Transactional
    public UserPaperStateResponse rate(Long paperId, Integer rating, UserResponse user) {
        Long userId = requireUserId(user);
        ensureActivePaper(paperId);
        validateRating(rating);

        ratingMapper.upsertRating(userId, paperId, rating);
        return state(paperId, userId);
    }

    public UserPaperStateResponse state(Long paperId, UserResponse user) {
        Long userId = requireUserId(user);
        ensureActivePaper(paperId);
        return state(paperId, userId);
    }

    private UserPaperStateResponse state(Long paperId, Long userId) {
        PaperFavorite favorite = favoriteMapper.selectByUserAndPaper(userId, paperId);
        PaperRating rating = ratingMapper.selectByUserAndPaper(userId, paperId);
        boolean favorited = favorite != null && FAVORITE_ACTIVE.equals(favorite.getStatus());
        Integer ratingValue = rating == null ? null : rating.getRating();
        return new UserPaperStateResponse(paperId, favorited, ratingValue);
    }

    private void ensureActivePaper(Long paperId) {
        if (paperId == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        Paper paper = paperMapper.selectActiveById(paperId);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }
    }

    private static Long requireUserId(UserResponse user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("用户不能为空");
        }
        return user.getId();
    }

    private static void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("评分必须在 1 到 5 之间");
        }
    }
}
