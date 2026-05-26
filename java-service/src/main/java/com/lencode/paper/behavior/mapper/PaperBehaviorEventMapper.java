package com.lencode.paper.behavior.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lencode.paper.behavior.entity.PaperBehaviorEvent;
import com.lencode.paper.behavior.vo.BehaviorEventTypeCountResponse;
import com.lencode.paper.behavior.vo.BehaviorTopPaperResponse;

public interface PaperBehaviorEventMapper extends BaseMapper<PaperBehaviorEvent> {

    @Insert("INSERT IGNORE INTO paper_behavior_events "
            + "(event_id, user_id, paper_id, event_type, keyword, author, publish_year, tag_id, metadata, occurred_at) "
            + "VALUES "
            + "(#{eventId}, #{userId}, #{paperId}, #{eventType}, #{keyword}, #{author}, "
            + "#{publishYear}, #{tagId}, #{metadata}, #{occurredAt})")
    int insertIgnore(PaperBehaviorEvent event);

    @Select("SELECT COUNT(*) FROM paper_behavior_events")
    Long countAllEvents();

    @Select("SELECT event_type AS eventType, COUNT(*) AS total FROM paper_behavior_events "
            + "GROUP BY event_type ORDER BY total DESC, event_type ASC")
    List<BehaviorEventTypeCountResponse> selectEventTypeCounts();

    @Select("SELECT e.paper_id AS paperId, p.title AS title, COUNT(*) AS total "
            + "FROM paper_behavior_events e "
            + "INNER JOIN papers p ON p.id = e.paper_id "
            + "WHERE e.event_type = #{eventType} AND e.paper_id IS NOT NULL "
            + "GROUP BY e.paper_id, p.title "
            + "ORDER BY total DESC, e.paper_id ASC LIMIT #{limit}")
    List<BehaviorTopPaperResponse> selectTopPapersByEventType(
            @Param("eventType") String eventType,
            @Param("limit") int limit);
}
