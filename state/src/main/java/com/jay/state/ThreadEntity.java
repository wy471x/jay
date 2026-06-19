package com.jay.state;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent thread metadata — equivalent to Rust's ThreadMetadata.
 * Spring Data JDBC maps this record to the 'threads' table.
 */
@Table("threads")
public record ThreadEntity(
        @Id String id,
        String rolloutPath,
        String preview,
        boolean ephemeral,
        String modelProvider,
        long createdAt,
        long updatedAt,
        String status,
        String path,
        String cwd,
        String cliVersion,
        String source,
        String title,
        String sandboxPolicy,
        String approvalMode,
        boolean archived,
        Long archivedAt,
        String gitSha,
        String gitBranch,
        String gitOriginUrl,
        String memoryMode,
        Long currentLeafId
) {}
