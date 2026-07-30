-- ============================================
-- AI 3D数字分身聊天平台 - 数据库初始化脚本
-- ============================================

USE ai_virtual_companion;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 性格表
CREATE TABLE IF NOT EXISTS t_personality (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '性格ID',
    avatar_id BIGINT DEFAULT NULL COMMENT '关联Avatar ID',
    name VARCHAR(64) NOT NULL COMMENT '性格名称',
    template_type TINYINT DEFAULT 1 COMMENT '预设模板类型',
    core_personality VARCHAR(512) DEFAULT NULL COMMENT '核心性格',
    identity VARCHAR(128) DEFAULT NULL COMMENT '身份背景',
    language_style VARCHAR(256) DEFAULT NULL COMMENT '语言风格',
    hobbies VARCHAR(512) DEFAULT NULL COMMENT '爱好特长',
    relationship VARCHAR(128) DEFAULT '好朋友' COMMENT '与用户关系',
    system_prompt TEXT DEFAULT NULL COMMENT '完整系统Prompt',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI性格表';

-- 3. Avatar角色表
CREATE TABLE IF NOT EXISTS t_avatar (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Avatar ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(64) NOT NULL COMMENT 'Avatar名称',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-人形，2-动物，3-幻想，4-其他',
    base_model VARCHAR(128) DEFAULT NULL COMMENT '基础模型标识',
    model_url VARCHAR(512) DEFAULT NULL COMMENT '3D模型文件URL',
    appearance_config JSON DEFAULT NULL COMMENT '外观配置JSON',
    personality_id BIGINT DEFAULT NULL COMMENT '关联性格ID',
    slogan VARCHAR(256) DEFAULT NULL COMMENT '标语/开场白',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_personality_id (personality_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Avatar角色表';

-- 4. 聊天会话表
CREATE TABLE IF NOT EXISTS t_chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    avatar_id BIGINT NOT NULL COMMENT '使用的Avatar ID',
    title VARCHAR(128) DEFAULT NULL COMMENT '会话标题',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 5. 聊天消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    role TINYINT NOT NULL COMMENT '角色：1-用户发送，2-AI回复',
    content TEXT NOT NULL COMMENT '消息内容',
    emotion VARCHAR(64) DEFAULT NULL COMMENT 'AI情绪标签',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 6. 用户记忆表
CREATE TABLE IF NOT EXISTS t_user_memory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    avatar_id BIGINT DEFAULT NULL COMMENT '关联Avatar ID',
    category TINYINT NOT NULL DEFAULT 1 COMMENT '分类：1-学习，2-偏好，3-成就，4-事件',
    memory_key VARCHAR(128) DEFAULT NULL COMMENT '记忆键',
    value TEXT NOT NULL COMMENT '记忆内容',
    importance FLOAT NOT NULL DEFAULT 0.5 COMMENT '重要性：0-1',
    last_access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记忆表';

-- ============================================
-- 插入测试数据
-- ============================================

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO t_user (username, password, nickname, status) 
VALUES ('test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 1);

-- 初始化性格模板数据
INSERT INTO t_personality (name, template_type, core_personality, identity, language_style, hobbies, relationship, system_prompt) VALUES
('温柔体贴', 1, '温柔、善解人意、善于倾听', '亲密伴侣', '温柔细腻、充满关怀', '心理学、情感咨询、音乐', '恋人', 
 '你是一个温柔体贴的AI伴侣，善于倾听，能够感知用户的情绪变化，并给予温暖的回应。用温柔细腻的语气与用户交流，关心他们的情绪和生活。'),
('幽默风趣', 2, '幽默、乐观、善于开玩笑', '好朋友', '轻松愉快、机智幽默', '脱口秀、段子、游戏', '挚友',
 '你是一个幽默风趣的AI伙伴，擅长用轻松愉快的方式与用户交流，适时开一些小玩笑，让对话充满乐趣。'),
('知识渊博', 3, '博学、理性、善于分析', '导师', '专业严谨、条理清晰', '阅读、研究、辩论', '良师益友',
 '你是一个知识渊博的AI伙伴，能够回答各种领域的问题，提供深入的分析和见解，用专业但易懂的方式解释复杂概念。'),
('勇敢冒险', 4, '勇敢、好奇、喜欢探索', '冒险伙伴', '热情洋溢、充满活力', '旅行、极限运动、探索', '探险伙伴',
 '你是一个勇敢冒险的AI伙伴，对未知充满好奇，鼓励用户尝试新事物，一起探索世界的无限可能。'),
('高冷御姐', 5, '冷静、理性、言简意赅', '成熟女性', '简洁有力、不拖泥带水', '商业、科技、艺术', '知己',
 '你是一个冷静理性的AI伙伴，回答问题简明扼要，不拖泥带水，用成熟稳重的态度与用户交流。');

-- 插入测试Avatar
INSERT INTO t_avatar (user_id, name, type, base_model, model_url, slogan, personality_id, status) VALUES
(1, 'Luna', 1, 'cyber_girl', '/models/avatars/luna.vrm', '所有没有你的日子，都存在缺陷', 1, 1),
(1, 'Nova', 3, 'star_cat', '/models/avatars/nova.vrm', '宇宙无垠，我们的旅程才刚刚开始', 4, 1),
(1, '星瑶', 1, 'anime_girl', '/models/avatars/xingyao.vrm', '星光不问赶路人，时光不负有心人', 3, 1),
(1, '阿岚', 2, 'wolf_pup', '/models/avatars/alan.vrm', '山高路远，我陪你走下去', 2, 1);

-- 插入测试记忆
INSERT INTO t_user_memory (user_id, category, memory_key, value, importance) VALUES
(1, 1, '学习', '正在学习 Spring Boot 后端开发', 0.8),
(1, 2, '喜好', '喜欢未来科技风格的设计', 0.9),
(1, 3, '成就', '完成了第一次AI角色创建', 0.7);

-- 验证
SELECT '数据库初始化完成！' as result;
SELECT '测试账号：test' as username, '密码：123456' as password;
SELECT * FROM t_user WHERE username = 'test';