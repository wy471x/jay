package com.jay.state;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadRepository extends CrudRepository<ThreadEntity, String> {

    @Query("SELECT * FROM threads WHERE archived = 0 ORDER BY updated_at DESC LIMIT :limit")
    List<ThreadEntity> listActive(@Param("limit") int limit);

    @Query("SELECT * FROM threads ORDER BY updated_at DESC LIMIT :limit")
    List<ThreadEntity> listAll(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE threads SET archived = 1, archived_at = :archivedAt WHERE id = :id")
    void archive(@Param("id") String id, @Param("archivedAt") long archivedAt);

    @Modifying
    @Query("UPDATE threads SET archived = 0, archived_at = NULL WHERE id = :id")
    void unarchive(@Param("id") String id);

    @Modifying
    @Query("UPDATE threads SET title = :title WHERE id = :id")
    void setName(@Param("id") String id, @Param("title") String title);

    @Modifying
    @Query("UPDATE threads SET title = NULL WHERE id = :id")
    void clearName(@Param("id") String id);

    @Modifying
    @Query("UPDATE threads SET current_leaf_id = :leafId WHERE id = :id")
    void setCurrentLeafId(@Param("id") String id, @Param("leafId") Long leafId);
}
