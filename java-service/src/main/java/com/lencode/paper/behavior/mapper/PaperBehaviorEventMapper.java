package com.lencode.paper.behavior.mapper;

import org.apache.ibatis.annotations.Insert;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lencode.paper.behavior.entity.PaperBehaviorEvent;

public interface PaperBehaviorEventMapper extends BaseMapper<PaperBehaviorEvent> {

    @Insert("INSERT IGNORE INTO paper_behavior_events "
            + "(event_id, user_id, paper_id, event_type, keyword, author, publish_year, tag_id, metadata, occurred_at) "
            + "VALUES "
            + "(#{eventId}, #{userId}, #{paperId}, #{eventType}, #{keyword}, #{author}, "
            + "#{publishYear}, #{tagId}, #{metadata}, #{occurredAt})")
    int insertIgnore(PaperBehaviorEvent event);
}
