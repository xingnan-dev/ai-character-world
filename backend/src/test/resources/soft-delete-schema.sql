DROP TABLE IF EXISTS t_auth_session;
CREATE TABLE t_auth_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME,
    create_time DATETIME,
    update_time DATETIME
);

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    current_user_character_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_user_current_character (current_user_character_id)
);

DROP TABLE IF EXISTS t_avatar;
CREATE TABLE t_avatar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type TINYINT DEFAULT 1,
    gender TINYINT DEFAULT 2,
    base_model VARCHAR(100),
    model_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    appearance_config VARCHAR(4000),
    personality_id BIGINT,
    slogan VARCHAR(200),
    source_description VARCHAR(1000),
    generate_type TINYINT DEFAULT 0,
    generate_result VARCHAR(4000),
    template_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

DROP TABLE IF EXISTS t_avatar_asset;
CREATE TABLE t_avatar_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    asset_type TINYINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    thumbnail_url VARCHAR(500),
    gender TINYINT DEFAULT 0,
    style_tags VARCHAR(500),
    color_tags VARCHAR(500),
    supported_attributes VARCHAR(4000),
    description VARCHAR(500),
    license VARCHAR(100),
    source VARCHAR(200),
    is_official TINYINT DEFAULT 0,
    download_count INT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

DROP TABLE IF EXISTS t_chat_session;
CREATE TABLE t_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    avatar_id BIGINT,
    character_id BIGINT,
    character_snapshot VARCHAR(10000),
    character_snapshot_version INT,
    user_character_id BIGINT,
    user_character_snapshot VARCHAR(10000),
    user_character_snapshot_version INT,
    personality_id BIGINT,
    personality_snapshot VARCHAR(4000),
    personality_snapshot_version INT,
    title VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_chat_session_character (character_id),
    INDEX idx_chat_session_user_character (user_character_id)
);

DROP TABLE IF EXISTS t_chat_message;
CREATE TABLE t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    request_id VARCHAR(64),
    role TINYINT NOT NULL,
    content VARCHAR(4000) NOT NULL,
    emotion VARCHAR(50),
    status TINYINT NOT NULL DEFAULT 2,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completion_time DATETIME,
    CONSTRAINT uk_chat_message_session_request_role UNIQUE (session_id, request_id, role)
);

DROP TABLE IF EXISTS t_user_memory;
CREATE TABLE t_user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    avatar_id BIGINT,
    character_id BIGINT,
    category TINYINT DEFAULT 1,
    memory_key VARCHAR(100),
    value VARCHAR(4000) NOT NULL,
    importance REAL DEFAULT 0.5,
    last_access_time DATETIME,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_memory_user_active_importance (user_id, status, deleted, importance),
    INDEX idx_memory_user_key (user_id, memory_key, status, deleted)
    , INDEX idx_memory_scope (user_id, character_id, status, deleted, importance)
);

DROP TABLE IF EXISTS t_ai_usage;
CREATE TABLE t_ai_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    llm_request_id VARCHAR(64) NOT NULL UNIQUE,
    chat_message_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    user_id BIGINT,
    session_id BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms BIGINT NOT NULL,
    success TINYINT NOT NULL,
    error_code VARCHAR(50),
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_world_event;
DROP TABLE IF EXISTS t_world_round;
DROP TABLE IF EXISTS t_world_participant;
DROP TABLE IF EXISTS t_world;
DROP TABLE IF EXISTS t_character_relationship;
DROP TABLE IF EXISTS t_character;
CREATE TABLE t_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    character_type TINYINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    age SMALLINT,
    identity VARCHAR(200),
    core_personality VARCHAR(1000),
    current_goal VARCHAR(500),
    biography VARCHAR(5000),
    relationship_to_user VARCHAR(300),
    speaking_style VARCHAR(500),
    profile_config VARCHAR(10000),
    source_description VARCHAR(2000),
    generate_type TINYINT NOT NULL DEFAULT 0,
    visual_type TINYINT NOT NULL DEFAULT 0,
    avatar_id BIGINT,
    image_url VARCHAR(500),
    avatar_color VARCHAR(32),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_character_user_active_created (user_id, status, deleted, create_time),
    INDEX idx_character_avatar_id (avatar_id)
);

CREATE TABLE t_character_relationship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    stage VARCHAR(16) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    interaction_style VARCHAR(500) NOT NULL,
    recent_change VARCHAR(1000) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_character_relationship_scope UNIQUE (user_id, character_id),
    CONSTRAINT fk_character_relationship_character FOREIGN KEY (character_id) REFERENCES t_character(id),
    INDEX idx_character_relationship_character (character_id)
);

CREATE TABLE t_world (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    user_character_id BIGINT,
    name VARCHAR(100) NOT NULL,
    background VARCHAR(2000),
    rules VARCHAR(2000),
    atmosphere VARCHAR(500),
    scene VARCHAR(1000),
    source_description VARCHAR(2000),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_world_owner_active_created (owner_user_id, status, deleted, create_time),
    INDEX idx_world_user_character (user_character_id)
);

CREATE TABLE t_world_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    world_id BIGINT NOT NULL,
    participant_type TINYINT NOT NULL,
    source_character_id BIGINT NOT NULL,
    character_snapshot VARCHAR(10000) NOT NULL,
    display_order INT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME,
    CONSTRAINT fk_world_participant_world FOREIGN KEY (world_id) REFERENCES t_world(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_participant_character UNIQUE (world_id, source_character_id),
    CONSTRAINT uk_world_participant_order UNIQUE (world_id, display_order),
    INDEX idx_world_participant_order (world_id, deleted, display_order),
    INDEX idx_world_participant_source (source_character_id)
);

CREATE TABLE t_world_round (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    world_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    user_input TEXT NOT NULL,
    user_character_id BIGINT,
    user_character_snapshot VARCHAR(10000),
    user_character_snapshot_version INT,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    started_time DATETIME,
    completion_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    execution_version BIGINT NOT NULL DEFAULT 0,
    lease_until DATETIME,
    CONSTRAINT fk_world_round_world FOREIGN KEY (world_id) REFERENCES t_world(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_round_request UNIQUE (world_id, request_id),
    INDEX idx_world_round_status_created (world_id, status, create_time),
    INDEX idx_world_round_recovery (status, lease_until),
    INDEX idx_world_round_user_character (user_character_id)
);

CREATE TABLE t_world_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    participant_id BIGINT,
    event_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    completion_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_world_event_round FOREIGN KEY (round_id) REFERENCES t_world_round(id) ON DELETE CASCADE,
    CONSTRAINT uk_world_event_sequence UNIQUE (round_id, sequence_no),
    CONSTRAINT uk_world_event_participant UNIQUE (round_id, participant_id),
    INDEX idx_world_event_round_sequence (round_id, sequence_no),
    INDEX idx_world_event_participant (participant_id)
);

CREATE TABLE IF NOT EXISTS t_character_image_generation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    character_id BIGINT,
    request_id VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    prompt VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    image_path VARCHAR(500),
    image_url VARCHAR(500),
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, request_id)
);

CREATE TABLE IF NOT EXISTS t_character_growth (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, character_id BIGINT NOT NULL,
 growth_summary VARCHAR(1000) NOT NULL, behavior_adaptation VARCHAR(1000) NOT NULL,
 user_understanding VARCHAR(1000) NOT NULL, growth_direction VARCHAR(1000) NOT NULL,
 version INT NOT NULL DEFAULT 0, deleted TINYINT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL, update_time DATETIME NOT NULL,
 UNIQUE(user_id, character_id)
);
