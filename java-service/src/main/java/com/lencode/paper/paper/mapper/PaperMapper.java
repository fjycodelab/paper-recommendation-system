package com.lencode.paper.paper.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.entity.Paper;

public interface PaperMapper extends BaseMapper<Paper> {

    String COLUMNS = "p.id, p.title, p.authors, p.abstract_text, p.publish_year, p.source, "
            + "p.source_paper_id, p.doi, p.source_url, p.download_url, p.keywords, "
            + "p.citation_count, p.published_at, p.status, p.submitted_by, "
            + "p.deleted_at, p.created_at, p.updated_at";

    @Select("SELECT " + COLUMNS + " FROM papers p WHERE p.id = #{id} AND p.status = 'ACTIVE'")
    Paper selectActiveById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT ", COLUMNS,
            " FROM papers p",
            "<if test='search != null and search.tagId != null'>",
            " INNER JOIN paper_tags pt ON pt.paper_id = p.id",
            "</if>",
            " WHERE p.status = 'ACTIVE'",
            "<if test='search != null and search.title != null'>",
            " AND p.title LIKE CONCAT('%', #{search.title}, '%')",
            "</if>",
            "<if test='search != null and search.author != null'>",
            " AND p.authors LIKE CONCAT('%', #{search.author}, '%')",
            "</if>",
            "<if test='search != null and search.source != null'>",
            " AND p.source LIKE CONCAT('%', #{search.source}, '%')",
            "</if>",
            "<if test='search != null and search.abstractKeyword != null'>",
            " AND p.abstract_text LIKE CONCAT('%', #{search.abstractKeyword}, '%')",
            "</if>",
            "<if test='search != null and search.year != null'>",
            " AND p.publish_year = #{search.year}",
            "</if>",
            "<if test='search != null and search.tagId != null'>",
            " AND pt.tag_id = #{search.tagId}",
            "</if>",
            " ORDER BY p.id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Paper> selectActivePage(
            @Param("search") PaperSearchRequest search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM papers p",
            "<if test='search != null and search.tagId != null'>",
            " INNER JOIN paper_tags pt ON pt.paper_id = p.id",
            "</if>",
            " WHERE p.status = 'ACTIVE'",
            "<if test='search != null and search.title != null'>",
            " AND p.title LIKE CONCAT('%', #{search.title}, '%')",
            "</if>",
            "<if test='search != null and search.author != null'>",
            " AND p.authors LIKE CONCAT('%', #{search.author}, '%')",
            "</if>",
            "<if test='search != null and search.source != null'>",
            " AND p.source LIKE CONCAT('%', #{search.source}, '%')",
            "</if>",
            "<if test='search != null and search.abstractKeyword != null'>",
            " AND p.abstract_text LIKE CONCAT('%', #{search.abstractKeyword}, '%')",
            "</if>",
            "<if test='search != null and search.year != null'>",
            " AND p.publish_year = #{search.year}",
            "</if>",
            "<if test='search != null and search.tagId != null'>",
            " AND pt.tag_id = #{search.tagId}",
            "</if>",
            "</script>"
    })
    Long countActive(@Param("search") PaperSearchRequest search);

    @Select("SELECT " + COLUMNS + " FROM papers p WHERE p.status = 'DELETED' "
            + "ORDER BY p.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Paper> selectDeletedPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM papers p WHERE p.status = 'DELETED'")
    Long countDeleted();

    @Update("UPDATE papers SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status <> 'DELETED'")
    int softDeleteById(@Param("id") Long id);

    @Update("UPDATE papers SET status = 'ACTIVE', deleted_at = NULL, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
