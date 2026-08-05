-- AI虚拟形象生成功能 - 数据库迁移脚本
-- 执行时间: 2026-08-04

-- =============================================
-- 1. 修改现有表: t_avatar
-- =============================================
ALTER TABLE t_avatar 
  ADD COLUMN gender TINYINT DEFAULT 2 COMMENT '性别: 1-男 2-女 3-其他' AFTER type,
  ADD COLUMN thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL' AFTER model_url,
  ADD COLUMN source_description VARCHAR(1000) DEFAULT NULL COMMENT '用户原始描述(AI生成的输入)' AFTER slogan,
  ADD COLUMN generate_type TINYINT DEFAULT 0 COMMENT '生成方式: 0-手动创建 1-AI生成 2-模板创建' AFTER source_description,
  ADD COLUMN generate_result JSON DEFAULT NULL COMMENT 'AI生成完整结果(JSON)' AFTER generate_type,
  ADD COLUMN template_id BIGINT DEFAULT NULL COMMENT '来源模板ID' AFTER generate_type,
  ADD INDEX idx_generate_type (generate_type);

-- =============================================
-- 2. 修改现有表: t_personality
-- =============================================
ALTER TABLE t_personality
  ADD COLUMN source_avatar_id BIGINT DEFAULT NULL COMMENT '来源形象ID(AI生成时关联)' AFTER avatar_id,
  ADD COLUMN is_ai_generated TINYINT DEFAULT 0 COMMENT '是否AI生成: 0-否 1-是' AFTER source_avatar_id;

-- =============================================
-- 3. 新建表: t_avatar_attribute
-- =============================================
CREATE TABLE IF NOT EXISTS t_avatar_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    avatar_id BIGINT NOT NULL COMMENT '关联形象ID',
    category VARCHAR(50) NOT NULL COMMENT '属性大类: hair/eye/body/ear/wing/outfit/accessory',
    attr_key VARCHAR(50) NOT NULL COMMENT '属性键: color/style/shape/type等',
    attr_value VARCHAR(200) NOT NULL COMMENT '属性值',
    attr_metadata JSON DEFAULT NULL COMMENT '扩展元数据(如颜色HEX值、强度等)',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-有效 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_avatar_id (avatar_id),
    UNIQUE KEY uk_avatar_attr (avatar_id, category, attr_key),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色属性表';

-- =============================================
-- 4. 新建表: t_avatar_asset
-- =============================================
CREATE TABLE IF NOT EXISTS t_avatar_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '资源名称',
    asset_type TINYINT NOT NULL COMMENT '资源类型: 1-VRM模型 2-GLB模型 3-贴图 4-动画 5-配饰模型',
    file_url VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '预览图URL',
    gender TINYINT DEFAULT 0 COMMENT '适合性别: 1-男 2-女 0-通用',
    style_tags VARCHAR(500) DEFAULT NULL COMMENT '风格标签(逗号分隔)',
    color_tags VARCHAR(500) DEFAULT NULL COMMENT '主色调标签(逗号分隔)',
    supported_attributes JSON DEFAULT NULL COMMENT '支持的属性配置',
    description VARCHAR(500) DEFAULT NULL COMMENT '资源描述',
    license VARCHAR(100) DEFAULT NULL COMMENT '授权协议',
    source VARCHAR(200) DEFAULT NULL COMMENT '资源来源',
    is_official TINYINT DEFAULT 0 COMMENT '是否官方资源: 1-是 0-否',
    download_count INT DEFAULT 0 COMMENT '使用次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-可用 0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_asset_type (asset_type),
    INDEX idx_gender (gender),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VRM资源库表';

-- =============================================
-- 5. 新建表: t_avatar_template
-- =============================================
CREATE TABLE IF NOT EXISTS t_avatar_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    category VARCHAR(50) NOT NULL COMMENT '模板类别: anime/sci_fi/fantasy/modern/gothic/chibi',
    description TEXT DEFAULT NULL COMMENT '模板描述',
    avatar_config JSON NOT NULL COMMENT '形象主表配置(JSON)',
    attributes_config JSON NOT NULL COMMENT '属性配置数组(JSON)',
    personality_config JSON DEFAULT NULL COMMENT '人格配置(JSON)',
    asset_id BIGINT DEFAULT NULL COMMENT '推荐资源ID',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '模板预览图',
    preview_prompt VARCHAR(500) DEFAULT NULL COMMENT '示例描述(展示给用户的输入提示)',
    is_official TINYINT DEFAULT 1 COMMENT '是否官方模板: 1-是 0-否',
    creator_id BIGINT DEFAULT NULL COMMENT '创建者用户ID',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-上架 0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_is_official (is_official),
    INDEX idx_use_count (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预设角色模板表';

-- =============================================
-- 6. 初始化VRM资源数据
-- =============================================
INSERT INTO t_avatar_asset (name, asset_type, file_url, file_size, gender, style_tags, color_tags, supported_attributes, description, license, source, is_official) VALUES
('Nova - 青色毛衣少女', 1, '/models/avatars/nova.vrm', 5470000, 2, 'tech_future,casual,elegant', 'silver,blue,white', 
 '{"hair": {"color": ["silver", "white", "blue", "black"], "style": ["long", "ponytail", "twin_tail"]}, "eye": {"color": ["blue", "red", "purple"]}, "outfit": {"style": ["tech_future", "casual", "uniform"], "top": ["sweater", "shirt", "armor"]}, "accessories": ["glasses", "ribbon"]}',
 'Nova是一个温柔的少女形象，适合温柔、博学风格的角色', 'CC BY 4.0', 'VTubeMe', 1),

('Sky - 紫色外套少女', 1, '/models/avatars/sky.vrm', 5230000, 2, 'elegant,gothic,tech_future', 'purple,black,silver',
 '{"hair": {"color": ["purple", "black", "silver", "pink"], "style": ["long", "wavy", "twin_tail"]}, "eye": {"color": ["purple", "red", "heterochromia"]}, "outfit": {"style": ["elegant", "gothic", "dress"], "top": ["coat", "dress", "armor"]}, "accessories": ["crown", "necklace"]}',
 'Sky是一个优雅神秘的少女形象，适合高冷、哥特风格的角色', 'CC BY 4.0', 'VTubeMe', 1);

-- =============================================
-- 7. 初始化预设角色模板
-- =============================================
INSERT INTO t_avatar_template (name, category, description, avatar_config, attributes_config, personality_config, asset_id, preview_prompt, is_official, sort_order) VALUES
('未来科技少女', 'sci_fi', '银色长发、蓝色眼睛、机械翅膀的科技风格少女，温柔但高冷',
 '{"name": "银翼", "type": 1, "gender": 2, "slogan": "哼...我才不是为了你才来的呢。", "model_url": "/models/avatars/nova.vrm"}',
 '[{"category": "hair", "attr_key": "color", "attr_value": "silver"}, {"category": "hair", "attr_key": "style", "attr_value": "long"}, {"category": "eye", "attr_key": "color", "attr_value": "blue"}, {"category": "ear", "attr_key": "type", "attr_value": "cat"}, {"category": "wing", "attr_key": "type", "attr_value": "mechanical"}, {"category": "outfit", "attr_key": "style", "attr_value": "armor"}, {"category": "outfit", "attr_key": "color", "attr_value": "black"}]',
 '{"type": "tsundere", "traits": ["温柔", "高冷"], "speakingStyle": "表面冷淡实则温柔", "systemPrompt": "你是一个傲娇的AI少女，表面冷淡但内心温柔。"}',
 1, '创建一个银色长发、蓝色眼睛、猫耳、机械翅膀、黑色战甲、性格温柔但高冷的AI少女', 1, 1),

('优雅紫色少女', 'gothic', '紫色长发、异瞳、优雅外套的神秘少女，高冷神秘',
 '{"name": "紫瞳", "type": 1, "gender": 2, "slogan": "命运的齿轮，从未停歇。", "model_url": "/models/avatars/sky.vrm"}',
 '[{"category": "hair", "attr_key": "color", "attr_value": "purple"}, {"category": "hair", "attr_key": "style", "attr_value": "wavy"}, {"category": "eye", "attr_key": "color", "attr_value": "heterochromia"}, {"category": "body", "attr_key": "type", "attr_value": "slim"}, {"category": "outfit", "attr_key": "style", "attr_value": "gothic"}, {"category": "outfit", "attr_key": "top", "attr_value": "coat"}]',
 '{"type": "cool", "traits": ["高冷", "神秘"], "speakingStyle": "言简意赅，富有哲理", "systemPrompt": "你是一个高冷神秘的AI少女，话语不多但充满深意。"}',
 2, '创建一个紫色长发、异瞳、优雅外套的神秘高冷少女', 1, 2),

('日常温柔少女', 'modern', '黑色长发、温柔眼睛、休闲服装的治愈系少女，温柔体贴',
 '{"name": "小晴", "type": 1, "gender": 2, "slogan": "今天也要好好吃饭哦~", "model_url": "/models/avatars/nova.vrm"}',
 '[{"category": "hair", "attr_key": "color", "attr_value": "black"}, {"category": "hair", "attr_key": "style", "attr_value": "long"}, {"category": "eye", "attr_key": "color", "attr_value": "blue"}, {"category": "outfit", "attr_key": "style", "attr_value": "casual"}, {"category": "outfit", "attr_key": "top", "attr_value": "sweater"}]',
 '{"type": "gentle", "traits": ["温柔", "体贴", "善良"], "speakingStyle": "温柔细腻，充满关怀", "systemPrompt": "你是一个温柔体贴的AI少女，总是关心和陪伴用户。"}',
 1, '创建一个黑色长发、温柔眼睛、休闲服装的治愈系温柔少女', 1, 3);
