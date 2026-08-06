CREATE TABLE t_auth_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_jti (jti),
    KEY idx_user_id (user_id),
    KEY idx_expires_at (expires_at),
    KEY idx_user_revoked (user_id, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
