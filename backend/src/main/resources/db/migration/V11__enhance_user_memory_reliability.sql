CREATE INDEX idx_memory_user_active_importance
    ON t_user_memory (user_id, status, deleted, importance);

CREATE INDEX idx_memory_user_key
    ON t_user_memory (user_id, memory_key, status, deleted);
