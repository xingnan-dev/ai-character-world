ALTER TABLE t_chat_session
    ADD COLUMN user_character_id BIGINT DEFAULT NULL AFTER character_snapshot_version,
    ADD COLUMN user_character_snapshot JSON DEFAULT NULL AFTER user_character_id,
    ADD COLUMN user_character_snapshot_version INT DEFAULT NULL AFTER user_character_snapshot,
    ADD KEY idx_chat_session_user_character (user_character_id);
