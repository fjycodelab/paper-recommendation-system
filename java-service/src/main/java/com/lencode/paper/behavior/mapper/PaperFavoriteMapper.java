package com.lencode.paper.behavior.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lencode.paper.behavior.entity.PaperFavorite;

public interface PaperFavoriteMapper extends BaseMapper<PaperFavorite> {

    @Insert("INSERT INTO paper_favorites (user_id, paper_id, status) "
            + "VALUES (#{userId}, #{paperId}, 'ACTIVE') "
            + "ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP")
    int upsertActive(@Param("userId") Long userId, @Param("paperId") Long paperId);

    @Update("UPDATE paper_favorites SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP "
            + "WHERE user_id = #{userId} AND paper_id = #{paperId}")
    int cancel(@Param("userId") Long userId, @Param("paperId") Long paperId);

    @Select("SELECT id, user_id, paper_id, status, created_at, updated_at "
            + "FROM paper_favorites WHERE user_id = #{userId} AND paper_id = #{paperId}")
    PaperFavorite selectByUserAndPaper(@Param("userId") Long userId, @Param("paperId") Long paperId);

    @Select({
            "<script>",
            "SELECT paper_id FROM paper_favorites",
            " WHERE user_id = #{userId} AND status = 'ACTIVE'",
            " AND paper_id IN",
            "<foreach collection='paperIds' item='paperId' open='(' separator=',' close=')'>",
            "#{paperId}",
            "</foreach>",
            "</script>"
    })
    List<Long> selectActivePaperIdsForUserAndPaperIds(
            @Param("userId") Long userId,
            @Param("paperIds") List<Long> paperIds);

    @Select("SELECT pf.paper_id FROM paper_favorites pf "
            + "INNER JOIN papers p ON p.id = pf.paper_id AND p.status = 'ACTIVE' "
            + "WHERE pf.user_id = #{userId} AND pf.status = 'ACTIVE' "
            + "ORDER BY pf.updated_at DESC, pf.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Long> selectActivePaperIdsByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM paper_favorites pf "
            + "INNER JOIN papers p ON p.id = pf.paper_id AND p.status = 'ACTIVE' "
            + "WHERE pf.user_id = #{userId} AND pf.status = 'ACTIVE'")
    Long countActiveByUser(@Param("userId") Long userId);
}
