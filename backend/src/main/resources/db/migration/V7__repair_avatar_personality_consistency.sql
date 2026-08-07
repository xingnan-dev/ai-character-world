-- Repair only the explicitly identified avatars. No latest-personality lookup is used.
INSERT INTO t_personality (
    avatar_id,
    source_avatar_id,
    is_ai_generated,
    name,
    template_type,
    core_personality,
    identity,
    language_style,
    hobbies,
    relationship,
    system_prompt,
    status,
    deleted,
    create_time,
    update_time
)
SELECT
    a.id,
    a.id,
    0,
    CONCAT(a.name, '的人格'),
    1,
    '温柔、幽默、善解人意',
    '虚拟人',
    '自然、亲切',
    '陪伴、交流',
    '朋友',
    CONCAT('你是名为', a.name, '的AI虚拟伴侣。保持温柔、幽默、善解人意的性格，用自然、亲切的方式交流。'),
    1,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM t_avatar a
WHERE a.id IN (53, 54)
  AND a.deleted = 0
  AND a.personality_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM t_personality marker
      WHERE marker.avatar_id = a.id
        AND marker.source_avatar_id = a.id
        AND marker.name = CONCAT(a.name, '的人格')
        AND marker.deleted = 0
  );

UPDATE t_avatar a
JOIN t_personality p
    ON p.avatar_id = a.id
    AND p.source_avatar_id = a.id
    AND p.name = CONCAT(a.name, '的人格')
    AND p.status = 1
    AND p.deleted = 0
SET a.personality_id = p.id,
    a.update_time = CURRENT_TIMESTAMP
WHERE a.id IN (53, 54)
  AND a.personality_id IS NULL
  AND a.deleted = 0;
