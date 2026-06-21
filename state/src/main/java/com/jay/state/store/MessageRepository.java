package com.jay.state.store;

import com.jay.state.model.MessageEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends CrudRepository<MessageEntity, Long> {

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY created_at ASC")
    List<MessageEntity> findByThreadId(@Param("threadId") String threadId);

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND id > :afterId ORDER BY created_at ASC")
    List<MessageEntity> findByThreadIdAfter(@Param("threadId") String threadId, @Param("afterId") long afterId);

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND parent_entry_id = :parentId")
    List<MessageEntity> findChildren(@Param("threadId") String threadId, @Param("parentId") long parentId);

    @Query("""
        SELECT m1.* FROM messages m1
        LEFT JOIN messages m2 ON m1.id = m2.parent_entry_id
        WHERE m1.thread_id = :threadId AND m2.id IS NULL
        ORDER BY m1.created_at DESC
        """)
    List<MessageEntity> findLeafMessages(@Param("threadId") String threadId);
}
