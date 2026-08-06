-- Canonical schema for a new ai_virtual_companion database.
-- Existing non-empty databases are baselined at version 1 and reconciled by V2.

CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    avatar_url VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_personality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avatar_id BIGINT DEFAULT NULL,
    source_avatar_id BIGINT DEFAULT NULL,
    is_ai_generated TINYINT NOT NULL DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    template_type TINYINT NOT NULL DEFAULT 1,
    core_personality VARCHAR(500) DEFAULT NULL,
    identity VARCHAR(100) DEFAULT NULL,
    language_style VARCHAR(200) DEFAULT NULL,
    hobbies VARCHAR(500) DEFAULT NULL,
    relationship VARCHAR(100) DEFAULT NULL,
    system_prompt TEXT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_avatar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type TINYINT NOT NULL DEFAULT 1,
    gender TINYINT NOT NULL DEFAULT 2,
    base_model VARCHAR(100) DEFAULT NULL,
    model_url VARCHAR(500) DEFAULT NULL,
    thumbnail_url VARCHAR(500) DEFAULT NULL,
    appearance_config JSON DEFAULT NULL,
    personality_id BIGINT DEFAULT NULL,
    slogan VARCHAR(200) DEFAULT NULL,
    source_description VARCHAR(1000) DEFAULT NULL,
    generate_type TINYINT NOT NULL DEFAULT 0,
    generate_result JSON DEFAULT NULL,
    template_id BIGINT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_personality_id (personality_id),
    KEY idx_generate_type (generate_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_avatar_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avatar_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    attr_key VARCHAR(50) NOT NULL,
    attr_value VARCHAR(200) NOT NULL,
    attr_metadata JSON DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_avatar_attr (avatar_id, category, attr_key),
    KEY idx_avatar_id (avatar_id),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_avatar_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    asset_type TINYINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT NULL,
    thumbnail_url VARCHAR(500) DEFAULT NULL,
    gender TINYINT NOT NULL DEFAULT 0,
    style_tags VARCHAR(500) DEFAULT NULL,
    color_tags VARCHAR(500) DEFAULT NULL,
    supported_attributes JSON DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    license VARCHAR(100) DEFAULT NULL,
    source VARCHAR(200) DEFAULT NULL,
    is_official TINYINT NOT NULL DEFAULT 0,
    download_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_asset_type (asset_type),
    KEY idx_gender (gender),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_avatar_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT DEFAULT NULL,
    avatar_config JSON NOT NULL,
    attributes_config JSON NOT NULL,
    personality_config JSON DEFAULT NULL,
    asset_id BIGINT DEFAULT NULL,
    thumbnail_url VARCHAR(500) DEFAULT NULL,
    preview_prompt VARCHAR(500) DEFAULT NULL,
    is_official TINYINT NOT NULL DEFAULT 1,
    creator_id BIGINT DEFAULT NULL,
    use_count INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category (category),
    KEY idx_is_official (is_official),
    KEY idx_use_count (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    avatar_id BIGINT NOT NULL,
    title VARCHAR(100) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role TINYINT NOT NULL,
    content TEXT NOT NULL,
    emotion VARCHAR(50) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    avatar_id BIGINT DEFAULT NULL,
    category TINYINT NOT NULL DEFAULT 1,
    memory_key VARCHAR(100) DEFAULT NULL,
    value TEXT NOT NULL,
    importance FLOAT NOT NULL DEFAULT 0.5,
    last_access_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
