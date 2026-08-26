CREATE TABLE t_world_round (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    world_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    user_input TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    started_time DATETIME DEFAULT NULL,
    completion_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_world_round_world
        FOREIGN KEY (world_id) REFERENCES t_world(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_round_request UNIQUE (world_id, request_id),
    KEY idx_world_round_status_created (world_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_world_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    participant_id BIGINT DEFAULT NULL,
    event_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    completion_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_world_event_round
        FOREIGN KEY (round_id) REFERENCES t_world_round(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_event_sequence UNIQUE (round_id, sequence_no),
    KEY idx_world_event_round_sequence (round_id, sequence_no),
    KEY idx_world_event_participant (participant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
