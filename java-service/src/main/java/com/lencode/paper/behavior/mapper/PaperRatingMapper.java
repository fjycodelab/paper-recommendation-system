package com.lencode.paper.behavior.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lencode.paper.behavior.entity.PaperRating;

public interface PaperRatingMapper extends BaseMapper<PaperRating> {

    @Insert("INSERT INTO paper_ratings (user_id, paper_id, rating) "
            + "VALUES (#{userId}, #{paperId}, #{rating}) "
            + "ON DUPLICATE KEY UPDATE rating = VALUES(rating), updated_at = CURRENT_TIMESTAMP")
    int upsertRating(
            @Param("userId") Long userId,
            @Param("paperId") Long paperId,
            @Param("rating") Integer rating);

    @Select("SELECT id, user_id, paper_id, rating, created_at, updated_at "
            + "FROM paper_ratings WHERE user_id = #{userId} AND paper_id = #{paperId}")
    PaperRating selectByUserAndPaper(@Param("userId") Long userId, @Param("paperId") Long paperId);
}
