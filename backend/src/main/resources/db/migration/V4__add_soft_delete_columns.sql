-- Separate logical deletion from business status.
-- deleted: 0 = active row, 1 = logically deleted row.

DELIMITER $$

DROP PROCEDURE IF EXISTS avc_v4_add_column_if_missing$$
DROP PROCEDURE IF EXISTS avc_v4_add_index_if_missing$$

CREATE PROCEDURE avc_v4_add_column_if_missing(
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
        PREPARE avc_v4_stmt FROM @ddl;
        EXECUTE avc_v4_stmt;
        DEALLOCATE PREPARE avc_v4_stmt;
    END IF;
END$$

CREATE PROCEDURE avc_v4_add_index_if_missing(
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
        PREPARE avc_v4_stmt FROM @ddl;
        EXECUTE avc_v4_stmt;
        DEALLOCATE PREPARE avc_v4_stmt;
    END IF;
END$$

DELIMITER ;

CALL avc_v4_add_column_if_missing('t_user', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_personality', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_avatar', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_avatar_attribute', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_avatar_asset', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_avatar_template', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_chat_session', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');
CALL avc_v4_add_column_if_missing('t_chat_message', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `emotion`');
CALL avc_v4_add_column_if_missing('t_user_memory', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除 1-已删除'' AFTER `status`');

-- Preserve rows that the legacy schema already marked as deleted.
UPDATE t_user SET deleted = 1 WHERE status = 0;
UPDATE t_personality SET deleted = 1 WHERE status = 0;
UPDATE t_avatar SET deleted = 1 WHERE status = 0;
UPDATE t_avatar_attribute SET deleted = 1 WHERE status = 0;
UPDATE t_chat_session SET deleted = 1 WHERE status = 0;
UPDATE t_user_memory SET deleted = 1 WHERE status = 0;

-- Asset/template status remains an availability or publishing state.
CALL avc_v4_add_index_if_missing('t_user', 'idx_status_deleted', '`status`, `deleted`');
CALL avc_v4_add_index_if_missing('t_personality', 'idx_avatar_deleted', '`avatar_id`, `deleted`');
CALL avc_v4_add_index_if_missing('t_avatar', 'idx_user_deleted', '`user_id`, `deleted`');
CALL avc_v4_add_index_if_missing('t_avatar_attribute', 'idx_avatar_deleted', '`avatar_id`, `deleted`');
CALL avc_v4_add_index_if_missing('t_avatar_asset', 'idx_type_status_deleted', '`asset_type`, `status`, `deleted`');
CALL avc_v4_add_index_if_missing('t_avatar_template', 'idx_category_status_deleted', '`category`, `status`, `deleted`');
CALL avc_v4_add_index_if_missing('t_chat_session', 'idx_user_deleted', '`user_id`, `deleted`');
CALL avc_v4_add_index_if_missing('t_chat_message', 'idx_session_deleted', '`session_id`, `deleted`');
CALL avc_v4_add_index_if_missing('t_user_memory', 'idx_user_deleted', '`user_id`, `deleted`');

DROP PROCEDURE avc_v4_add_index_if_missing;
DROP PROCEDURE avc_v4_add_column_if_missing;
