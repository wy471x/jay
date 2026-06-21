package com.jay.state.store;

import com.jay.state.model.CheckpointEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckpointRepository extends CrudRepository<CheckpointEntity, Void> {

    @Query("SELECT * FROM checkpoints WHERE thread_id = :threadId ORDER BY created_at DESC")
    List<CheckpointEntity> findByThreadId(@Param("threadId") String threadId);

    @Query("SELECT * FROM checkpoints WHERE thread_id = :threadId AND checkpoint_id = :checkpointId")
    CheckpointEntity findOne(@Param("threadId") String threadId, @Param("checkpointId") String checkpointId);

    @Modifying
    @Query("DELETE FROM checkpoints WHERE thread_id = :threadId AND checkpoint_id = :checkpointId")
    void deleteOne(@Param("threadId") String threadId, @Param("checkpointId") String checkpointId);

    @Modifying
    @Query("DELETE FROM checkpoints WHERE thread_id = :threadId")
    void deleteByThreadId(@Param("threadId") String threadId);
}
