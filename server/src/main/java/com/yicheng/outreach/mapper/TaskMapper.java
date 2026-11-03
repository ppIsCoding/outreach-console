package com.yicheng.outreach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yicheng.outreach.model.entity.Task;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务 Mapper。
 */
public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT status, COUNT(*) AS cnt FROM task GROUP BY status")
    List<StatusCountRow> countByStatus();

    @Select("SELECT MAX(id) FROM task WHERE id LIKE CONCAT(#{prefix}, '%')")
    String selectMaxIdByPrefix(@Param("prefix") String prefix);

    /**
     * 按 startedAt 聚合每日任务指标，用于概览趋势与环比。
     */
    @Select("""
            SELECT CAST(started_at AS DATE) AS stat_date,
                   COUNT(*) AS task_count,
                   COALESCE(SUM(called_count), 0) AS called_count,
                   COALESCE(SUM(connected_count), 0) AS connected_count,
                   COALESCE(SUM(intent_count), 0) AS intent_count,
                   COALESCE(SUM(total_duration_sec), 0) AS duration_sec
            FROM task
            WHERE started_at >= #{start} AND started_at < #{end}
            GROUP BY CAST(started_at AS DATE)
            ORDER BY stat_date
            """)
    List<DailyStatRow> selectDailyStats(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    /**
     * 状态流转 + 乐观锁：WHERE id = ? AND version = ?。
     */
    @Update("""
            <script>
            UPDATE task
            SET status = #{status},
                version = version + 1,
                updated_at = #{updatedAt}
                <if test="scheduledAt != null">, scheduled_at = #{scheduledAt}</if>
                <if test="startedAt != null">, started_at = #{startedAt}</if>
                <if test="finishedAt != null">, finished_at = #{finishedAt}</if>
            WHERE id = #{id} AND version = #{version}
            </script>
            """)
    int updateStatusWithVersion(@Param("id") String id,
                                @Param("status") TaskStatus status,
                                @Param("version") Integer version,
                                @Param("scheduledAt") LocalDateTime scheduledAt,
                                @Param("startedAt") LocalDateTime startedAt,
                                @Param("finishedAt") LocalDateTime finishedAt,
                                @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 定时模拟器推进进度 + 乐观锁。
     */
    @Update("""
            <script>
            UPDATE task
            SET called_count = #{calledCount},
                connected_count = #{connectedCount},
                intent_count = #{intentCount},
                total_duration_sec = #{totalDurationSec},
                status = #{status},
                version = version + 1,
                updated_at = #{updatedAt}
                <if test="finishedAt != null">, finished_at = #{finishedAt}</if>
            WHERE id = #{id} AND version = #{version}
            </script>
            """)
    int updateProgressWithVersion(@Param("id") String id,
                                  @Param("calledCount") Integer calledCount,
                                  @Param("connectedCount") Integer connectedCount,
                                  @Param("intentCount") Integer intentCount,
                                  @Param("totalDurationSec") Integer totalDurationSec,
                                  @Param("status") TaskStatus status,
                                  @Param("version") Integer version,
                                  @Param("finishedAt") LocalDateTime finishedAt,
                                  @Param("updatedAt") LocalDateTime updatedAt);
}
