ALTER TABLE t_user
    ADD COLUMN current_user_character_id BIGINT DEFAULT NULL AFTER avatar_url,
    ADD KEY idx_user_current_character (current_user_character_id);

ALTER TABLE t_world
    ADD COLUMN user_character_id BIGINT DEFAULT NULL AFTER owner_user_id,
    ADD KEY idx_world_user_character (user_character_id);

ALTER TABLE t_world_round
    ADD COLUMN user_character_id BIGINT DEFAULT NULL AFTER user_input,
    ADD COLUMN user_character_snapshot JSON DEFAULT NULL AFTER user_character_id,
    ADD COLUMN user_character_snapshot_version INT DEFAULT NULL AFTER user_character_snapshot,
    ADD KEY idx_world_round_user_character (user_character_id);

ALTER TABLE t_chat_session
    MODIFY COLUMN avatar_id BIGINT NULL,
    ADD COLUMN character_id BIGINT DEFAULT NULL AFTER avatar_id,
    ADD COLUMN character_snapshot JSON DEFAULT NULL AFTER character_id,
    ADD COLUMN character_snapshot_version INT DEFAULT NULL AFTER character_snapshot,
    ADD KEY idx_chat_session_character (character_id);
