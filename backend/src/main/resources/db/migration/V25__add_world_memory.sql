CREATE TABLE t_world_memory (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, world_id BIGINT NOT NULL,
 memory_type VARCHAR(32) NOT NULL, memory_key VARCHAR(255), content VARCHAR(2000) NOT NULL, dedupe_hash CHAR(64) NOT NULL,
 source_round_id BIGINT, source_event_id BIGINT, importance INT NOT NULL DEFAULT 50,
 version INT NOT NULL DEFAULT 0, deleted TINYINT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
 INDEX idx_world_memory_scope(user_id, world_id), INDEX idx_world_memory_type(world_id, memory_type),
 INDEX idx_world_memory_round(source_round_id),
 CONSTRAINT uk_world_memory_exact UNIQUE(user_id, world_id, memory_type, dedupe_hash),
 CONSTRAINT fk_world_memory_world FOREIGN KEY(world_id) REFERENCES t_world(id),
 CONSTRAINT fk_world_memory_round FOREIGN KEY(source_round_id) REFERENCES t_world_round(id),
 CONSTRAINT fk_world_memory_event FOREIGN KEY(source_event_id) REFERENCES t_world_event(id)
);
