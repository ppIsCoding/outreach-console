package com.yicheng.outreach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yicheng.outreach.model.entity.CallResult;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通话结果 Mapper。
 */
public interface CallResultMapper extends BaseMapper<CallResult> {

    @Select("""
            SELECT call_status AS name, COUNT(*) AS cnt
            FROM call_result
            WHERE task_id = #{taskId}
            GROUP BY call_status
            """)
    List<NameCountRow> countByResult(@Param("taskId") String taskId);

    @Select("""
            SELECT intent_level AS name, COUNT(*) AS cnt
            FROM call_result
            WHERE task_id = #{taskId} AND intent_level IS NOT NULL
            GROUP BY intent_level
            """)
    List<NameCountRow> countByIntent(@Param("taskId") String taskId);
}
