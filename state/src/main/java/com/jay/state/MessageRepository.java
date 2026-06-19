package com.jay.state;

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
}
