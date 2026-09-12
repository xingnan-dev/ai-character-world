CREATE TABLE t_character_relationship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    stage VARCHAR(16) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    interaction_style VARCHAR(500) NOT NULL,
    recent_change VARCHAR(1000) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_character_relationship_scope UNIQUE (user_id, character_id),
    CONSTRAINT fk_character_relationship_character FOREIGN KEY (character_id) REFERENCES t_character(id),
    KEY idx_character_relationship_character (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
