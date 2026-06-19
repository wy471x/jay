package com.jay.state;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicToolRepository extends CrudRepository<DynamicToolEntity, Void> {

    @Query("SELECT * FROM thread_dynamic_tools WHERE thread_id = :threadId ORDER BY position")
    List<DynamicToolEntity> findByThreadId(@Param("threadId") String threadId);

    @Modifying
    @Query("DELETE FROM thread_dynamic_tools WHERE thread_id = :threadId")
    void deleteByThreadId(@Param("threadId") String threadId);
}
