CREATE TABLE t_character_image_generation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    character_id BIGINT NULL,
    request_id VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    prompt VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    image_path VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_character_image_request (user_id, request_id),
    KEY idx_character_image_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
