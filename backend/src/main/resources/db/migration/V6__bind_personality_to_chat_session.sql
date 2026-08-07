ALTER TABLE t_chat_session
    ADD COLUMN personality_id BIGINT DEFAULT NULL AFTER avatar_id,
    ADD COLUMN personality_snapshot JSON DEFAULT NULL AFTER personality_id,
    ADD COLUMN personality_snapshot_version INT DEFAULT NULL AFTER personality_snapshot,
    ADD KEY idx_chat_session_personality_id (personality_id);

-- Existing sessions are backfilled only when the avatar has an explicit personality binding.
-- Sessions without an unambiguous binding remain NULL and must not guess a "latest" personality.
UPDATE t_chat_session s
JOIN t_avatar a
    ON a.id = s.avatar_id
    AND a.user_id = s.user_id
    AND a.personality_id IS NOT NULL
    AND a.deleted = 0
JOIN t_personality p
    ON p.id = a.personality_id
    AND p.avatar_id = a.id
    AND p.status = 1
    AND p.deleted = 0
SET s.personality_id = p.id,
    s.personality_snapshot = JSON_OBJECT(
        'personalityId', p.id,
        'avatarId', a.id,
        'name', p.name,
        'corePersonality', p.core_personality,
        'identity', p.identity,
        'languageStyle', p.language_style,
        'hobbies', p.hobbies,
        'relationship', p.relationship,
        'snapshotVersion', 1
    ),
    s.personality_snapshot_version = 1
WHERE s.personality_id IS NULL
  AND s.personality_snapshot IS NULL
  AND s.deleted = 0;
