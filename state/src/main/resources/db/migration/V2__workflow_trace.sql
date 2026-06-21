CREATE TABLE IF NOT EXISTS workflow_runs (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    goal TEXT NOT NULL,
    status TEXT NOT NULL,
    input_hash TEXT,
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    metadata_json TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_workflow_runs_status_started_at
    ON workflow_runs(status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_runs_workflow_started_at
    ON workflow_runs(workflow_id, started_at DESC);

CREATE TABLE IF NOT EXISTS branch_runs (
    id TEXT PRIMARY KEY,
    workflow_run_id TEXT NOT NULL,
    branch_id TEXT NOT NULL,
    node_id TEXT NOT NULL,
    status TEXT NOT NULL,
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    result_json TEXT NOT NULL DEFAULT '{}',
    FOREIGN KEY(workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_branch_runs_workflow_run_id
    ON branch_runs(workflow_run_id);
CREATE INDEX IF NOT EXISTS idx_branch_runs_branch_id
    ON branch_runs(branch_id);

CREATE TABLE IF NOT EXISTS leaf_runs (
    id TEXT PRIMARY KEY,
    workflow_run_id TEXT NOT NULL,
    branch_run_id TEXT,
    leaf_id TEXT NOT NULL,
    task_id TEXT NOT NULL,
    input_hash TEXT,
    status TEXT NOT NULL,
    output_json TEXT NOT NULL DEFAULT '{}',
    artifacts_json TEXT NOT NULL DEFAULT '[]',
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    FOREIGN KEY(workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE,
    FOREIGN KEY(branch_run_id) REFERENCES branch_runs(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_leaf_runs_workflow_run_id
    ON leaf_runs(workflow_run_id);
CREATE INDEX IF NOT EXISTS idx_leaf_runs_replay_lookup
    ON leaf_runs(workflow_run_id, leaf_id, input_hash);

CREATE TABLE IF NOT EXISTS control_node_runs (
    id TEXT PRIMARY KEY,
    workflow_run_id TEXT NOT NULL,
    node_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    status TEXT NOT NULL,
    selected_children_json TEXT NOT NULL DEFAULT '[]',
    result_json TEXT NOT NULL DEFAULT '{}',
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    FOREIGN KEY(workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_control_node_runs_workflow_run_id
    ON control_node_runs(workflow_run_id);
CREATE INDEX IF NOT EXISTS idx_control_node_runs_node_id
    ON control_node_runs(node_id);

CREATE TABLE IF NOT EXISTS teacher_candidates (
    id TEXT PRIMARY KEY,
    workflow_run_id TEXT NOT NULL,
    control_node_run_id TEXT NOT NULL,
    candidate_id TEXT NOT NULL,
    branch_run_id TEXT,
    score REAL,
    passed INTEGER,
    rationale_json TEXT NOT NULL DEFAULT '{}',
    created_at INTEGER NOT NULL,
    FOREIGN KEY(workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE,
    FOREIGN KEY(control_node_run_id) REFERENCES control_node_runs(id) ON DELETE CASCADE,
    FOREIGN KEY(branch_run_id) REFERENCES branch_runs(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_teacher_candidates_workflow_run_id
    ON teacher_candidates(workflow_run_id);
CREATE INDEX IF NOT EXISTS idx_teacher_candidates_control_node_run_id
    ON teacher_candidates(control_node_run_id);
