package com.jay.state;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalRepository extends CrudRepository<GoalEntity, String> {

    @Modifying
    @Query("UPDATE thread_goals SET tokens_used = tokens_used + :tokens, " +
           "time_used_seconds = time_used_seconds + :seconds, " +
           "continuation_count = CASE WHEN :isContinuation THEN continuation_count + 1 ELSE continuation_count END, " +
           "updated_at = :now WHERE thread_id = :threadId")
    void recordProgress(@Param("threadId") String threadId, @Param("tokens") long tokens,
                        @Param("seconds") long seconds, @Param("isContinuation") boolean isContinuation,
                        @Param("now") long now);
}
