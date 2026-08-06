-- Bring databases created by the legacy schema/init scripts up to the V1 shape.
-- Metadata guards make this safe when the former manual AI migration was already run.

DELIMITER $$

DROP PROCEDURE IF EXISTS avc_add_column_if_missing$$
DROP PROCEDURE IF EXISTS avc_add_index_if_missing$$

CREATE PROCEDURE avc_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table_name
           AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'),
            '` ', p_definition
        );
        PREPARE avc_stmt FROM @ddl;
        EXECUTE avc_stmt;
        DEALLOCATE PREPARE avc_stmt;
    END IF;
END$$

CREATE PROCEDURE avc_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table_name
           AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD INDEX `', REPLACE(p_index_name, '`', '``'),
            '` (', p_columns, ')'
        );
        PREPARE avc_stmt FROM @ddl;
        EXECUTE avc_stmt;
        DEALLOCATE PREPARE avc_stmt;
    END IF;
END$$

DELIMITER ;

CALL avc_add_column_if_missing('t_avatar', 'gender',
    'TINYINT NOT NULL DEFAULT 2 COMMENT ''性别: 1-男 2-女 3-其他'' AFTER `type`');
CALL avc_add_column_if_missing('t_avatar', 'thumbnail_url',
    'VARCHAR(500) DEFAULT NULL COMMENT ''缩略图URL'' AFTER `model_url`');
CALL avc_add_column_if_missing('t_avatar', 'source_description',
    'VARCHAR(1000) DEFAULT NULL COMMENT ''用户原始描述'' AFTER `slogan`');
CALL avc_add_column_if_missing('t_avatar', 'generate_type',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''生成方式: 0-手动 1-AI 2-模板'' AFTER `source_description`');
CALL avc_add_column_if_missing('t_avatar', 'generate_result',
    'JSON DEFAULT NULL COMMENT ''AI生成完整结果'' AFTER `generate_type`');
CALL avc_add_column_if_missing('t_avatar', 'template_id',
    'BIGINT DEFAULT NULL COMMENT ''来源模板ID'' AFTER `generate_result`');
CALL avc_add_index_if_missing('t_avatar', 'idx_generate_type', '`generate_type`');

CALL avc_add_column_if_missing('t_personality', 'source_avatar_id',
    'BIGINT DEFAULT NULL COMMENT ''来源形象ID'' AFTER `avatar_id`');
CALL avc_add_column_if_missing('t_personality', 'is_ai_generated',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否AI生成'' AFTER `source_avatar_id`');

DROP PROCEDURE avc_add_index_if_missing;
DROP PROCEDURE avc_add_column_if_missing;

CREATE TABLE IF NOT EXISTS t_avatar_attribute (
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

CREATE TABLE IF NOT EXISTS t_avatar_asset (
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

CREATE TABLE IF NOT EXISTS t_avatar_template (
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
