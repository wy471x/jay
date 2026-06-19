package com.jay.state;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends CrudRepository<JobEntity, String> {

    @Query("SELECT * FROM jobs ORDER BY updated_at DESC LIMIT :limit")
    List<JobEntity> listRecent(@Param("limit") int limit);
}
