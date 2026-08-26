CREATE TABLE t_world (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    background VARCHAR(2000) DEFAULT NULL,
    rules VARCHAR(2000) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_world_owner_active_created (owner_user_id, status, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_world_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    world_id BIGINT NOT NULL,
    participant_type TINYINT NOT NULL,
    source_character_id BIGINT NOT NULL,
    character_snapshot JSON NOT NULL,
    display_order INT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_world_participant_world
        FOREIGN KEY (world_id) REFERENCES t_world(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_participant_character UNIQUE (world_id, source_character_id),
    CONSTRAINT uk_world_participant_order UNIQUE (world_id, display_order),
    KEY idx_world_participant_order (world_id, deleted, display_order),
    KEY idx_world_participant_source (source_character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
