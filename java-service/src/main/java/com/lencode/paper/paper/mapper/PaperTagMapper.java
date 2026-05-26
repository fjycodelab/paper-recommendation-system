package com.lencode.paper.paper.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.lencode.paper.tag.entity.ResearchTag;

public interface PaperTagMapper {

    @Insert("INSERT INTO paper_tags (paper_id, tag_id) VALUES (#{paperId}, #{tagId})")
    int insertTag(@Param("paperId") Long paperId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM paper_tags WHERE paper_id = #{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT t.id, t.code, t.parent_id, t.name, t.level, t.status, t.sort_order, "
            + "t.created_at, t.updated_at "
            + "FROM research_tags t INNER JOIN paper_tags pt ON pt.tag_id = t.id "
            + "WHERE pt.paper_id = #{paperId} ORDER BY t.sort_order ASC, t.id ASC")
    List<ResearchTag> selectTagsByPaperId(@Param("paperId") Long paperId);
}
