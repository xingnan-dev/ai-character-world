ALTER TABLE t_user_memory ADD COLUMN character_id BIGINT NULL AFTER avatar_id;
CREATE INDEX idx_user_memory_scope ON t_user_memory (user_id, character_id, status, deleted, importance);
