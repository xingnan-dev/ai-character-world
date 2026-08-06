-- Idempotent reference data. Never delete or overwrite user-managed rows.

INSERT INTO t_avatar_asset (
    name, asset_type, file_url, file_size, gender, style_tags, color_tags,
    supported_attributes, description, license, source, is_official, status
)
SELECT
    'Nova - 科技少女', 1, '/models/avatars/nova.vrm', 5470000, 2,
    'tech_future,cyberpunk,casual', 'silver,blue,white,cyan',
    '{"hair":{"color":["silver","white","blue","cyan"],"style":["long","ponytail","twin_tail","short"]},"eye":{"color":["blue","cyan","green"]},"outfit":{"style":["tech_future","casual","uniform","armor"]},"ear":["cat","human"],"wing":["mechanical","none"]}',
    'Nova是一个科技感少女形象，适合机甲、科技、休闲风格的角色',
    'CC BY 4.0', 'VTubeMe', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_avatar_asset WHERE file_url = '/models/avatars/nova.vrm'
);

INSERT INTO t_avatar_asset (
    name, asset_type, file_url, file_size, gender, style_tags, color_tags,
    supported_attributes, description, license, source, is_official, status
)
SELECT
    'Sky - 神秘少女', 1, '/models/avatars/sky.vrm', 5230000, 2,
    'elegant,gothic,mysterious,dark', 'purple,black,crimson,violet,red',
    '{"hair":{"color":["purple","black","violet","crimson","red"],"style":["long","wavy","twin_tail","ponytail"]},"eye":{"color":["purple","red","heterochromia"]},"outfit":{"style":["elegant","gothic","dress","coat"]},"ear":["elf","human"],"wing":["angel","demon","none"]}',
    'Sky是一个优雅神秘的少女形象，适合哥特、优雅、神秘风格的角色',
    'CC BY 4.0', 'VTubeMe', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_avatar_asset WHERE file_url = '/models/avatars/sky.vrm'
);

INSERT INTO t_avatar_template (
    name, category, description, avatar_config, attributes_config,
    personality_config, asset_id, preview_prompt, is_official, sort_order, status
)
SELECT
    '未来科技少女', 'sci_fi', '银色长发、蓝色眼睛、机械翅膀的科技风格少女，温柔但高冷',
    '{"name":"银翼","type":1,"gender":2,"slogan":"哼...我才不是为了你才来的呢。","model_url":"/models/avatars/nova.vrm"}',
    '[{"category":"hair","attr_key":"color","attr_value":"silver"},{"category":"hair","attr_key":"style","attr_value":"long"},{"category":"eye","attr_key":"color","attr_value":"blue"},{"category":"wing","attr_key":"type","attr_value":"mechanical"},{"category":"outfit","attr_key":"style","attr_value":"armor"}]',
    '{"type":"tsundere","traits":["温柔","高冷"],"speakingStyle":"表面冷淡实则温柔"}',
    (SELECT id FROM t_avatar_asset WHERE file_url = '/models/avatars/nova.vrm' ORDER BY id LIMIT 1),
    '创建一个银色长发、蓝色眼睛、机械翅膀、性格温柔但高冷的AI少女', 1, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_avatar_template WHERE name = '未来科技少女' AND is_official = 1
);

INSERT INTO t_avatar_template (
    name, category, description, avatar_config, attributes_config,
    personality_config, asset_id, preview_prompt, is_official, sort_order, status
)
SELECT
    '优雅紫色少女', 'gothic', '紫色长发、异瞳、优雅外套的神秘少女，高冷神秘',
    '{"name":"紫瞳","type":1,"gender":2,"slogan":"命运的齿轮，从未停歇。","model_url":"/models/avatars/sky.vrm"}',
    '[{"category":"hair","attr_key":"color","attr_value":"purple"},{"category":"hair","attr_key":"style","attr_value":"wavy"},{"category":"eye","attr_key":"color","attr_value":"heterochromia"},{"category":"outfit","attr_key":"style","attr_value":"gothic"}]',
    '{"type":"cool","traits":["高冷","神秘"],"speakingStyle":"言简意赅，富有哲理"}',
    (SELECT id FROM t_avatar_asset WHERE file_url = '/models/avatars/sky.vrm' ORDER BY id LIMIT 1),
    '创建一个紫色长发、异瞳、优雅外套的神秘高冷少女', 1, 2, 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_avatar_template WHERE name = '优雅紫色少女' AND is_official = 1
);

INSERT INTO t_avatar_template (
    name, category, description, avatar_config, attributes_config,
    personality_config, asset_id, preview_prompt, is_official, sort_order, status
)
SELECT
    '日常温柔少女', 'modern', '黑色长发、温柔眼睛、休闲服装的治愈系少女，温柔体贴',
    '{"name":"小晴","type":1,"gender":2,"slogan":"今天也要好好吃饭哦~","model_url":"/models/avatars/nova.vrm"}',
    '[{"category":"hair","attr_key":"color","attr_value":"black"},{"category":"hair","attr_key":"style","attr_value":"long"},{"category":"eye","attr_key":"color","attr_value":"blue"},{"category":"outfit","attr_key":"style","attr_value":"casual"}]',
    '{"type":"gentle","traits":["温柔","体贴","善良"],"speakingStyle":"温柔细腻，充满关怀"}',
    (SELECT id FROM t_avatar_asset WHERE file_url = '/models/avatars/nova.vrm' ORDER BY id LIMIT 1),
    '创建一个黑色长发、温柔眼睛、休闲服装的治愈系温柔少女', 1, 3, 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_avatar_template WHERE name = '日常温柔少女' AND is_official = 1
);

INSERT INTO t_personality (
    name, template_type, core_personality, identity, language_style,
    hobbies, relationship, system_prompt, status
)
SELECT
    '温柔体贴', 1, '温柔、善解人意、善于倾听', '亲密伴侣',
    '温柔细腻、充满关怀', '心理学、情感咨询、音乐', '恋人',
    '你是一个温柔体贴的AI伴侣，善于倾听，并给予温暖的回应。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_personality WHERE avatar_id IS NULL AND name = '温柔体贴'
);

INSERT INTO t_personality (
    name, template_type, core_personality, identity, language_style,
    hobbies, relationship, system_prompt, status
)
SELECT
    '幽默风趣', 2, '幽默、乐观、善于开玩笑', '好朋友',
    '轻松愉快、机智幽默', '脱口秀、段子、游戏', '挚友',
    '你是一个幽默风趣的AI伙伴，用轻松愉快的方式与用户交流。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_personality WHERE avatar_id IS NULL AND name = '幽默风趣'
);

INSERT INTO t_personality (
    name, template_type, core_personality, identity, language_style,
    hobbies, relationship, system_prompt, status
)
SELECT
    '知识渊博', 3, '博学、理性、善于分析', '导师',
    '专业严谨、条理清晰', '阅读、研究、辩论', '良师益友',
    '你是一个知识渊博的AI伙伴，用专业但易懂的方式解释复杂概念。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_personality WHERE avatar_id IS NULL AND name = '知识渊博'
);

INSERT INTO t_personality (
    name, template_type, core_personality, identity, language_style,
    hobbies, relationship, system_prompt, status
)
SELECT
    '勇敢冒险', 4, '勇敢、好奇、喜欢探索', '冒险伙伴',
    '热情洋溢、充满活力', '旅行、极限运动、探索', '探险伙伴',
    '你是一个勇敢冒险的AI伙伴，鼓励用户尝试新事物。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_personality WHERE avatar_id IS NULL AND name = '勇敢冒险'
);

INSERT INTO t_personality (
    name, template_type, core_personality, identity, language_style,
    hobbies, relationship, system_prompt, status
)
SELECT
    '高冷御姐', 5, '冷静、理性、言简意赅', '成熟女性',
    '简洁有力、不拖泥带水', '商业、科技、艺术', '知己',
    '你是一个冷静理性的AI伙伴，用成熟稳重的态度与用户交流。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_personality WHERE avatar_id IS NULL AND name = '高冷御姐'
);
