-- AI虚拟陪伴平台数据库表结构
-- 数据库: ai_virtual_companion

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(加密)',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 性格表
CREATE TABLE IF NOT EXISTS t_personality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    avatar_id BIGINT DEFAULT NULL COMMENT '关联头像ID',
    name VARCHAR(50) NOT NULL COMMENT '性格名称',
    template_type TINYINT DEFAULT 1 COMMENT '模板类型',
    core_personality VARCHAR(500) DEFAULT NULL COMMENT '核心性格',
    identity VARCHAR(100) DEFAULT NULL COMMENT '身份设定',
    language_style VARCHAR(200) DEFAULT NULL COMMENT '语言风格',
    hobbies VARCHAR(500) DEFAULT NULL COMMENT '兴趣爱好',
    relationship VARCHAR(100) DEFAULT NULL COMMENT '与用户关系',
    system_prompt TEXT DEFAULT NULL COMMENT '系统提示词',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='性格配置表';

-- 头像表
CREATE TABLE IF NOT EXISTS t_avatar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '形象名称',
    type TINYINT DEFAULT 1 COMMENT '类型: 1-人类 2-动物 3-幻想 4-其他',
    base_model VARCHAR(100) DEFAULT NULL COMMENT '基础模型名称',
    model_url VARCHAR(500) DEFAULT NULL COMMENT '模型文件URL',
    appearance_config JSON DEFAULT NULL COMMENT '外观配置(JSON)',
    personality_id BIGINT DEFAULT NULL COMMENT '性格ID',
    slogan VARCHAR(200) DEFAULT NULL COMMENT '标语/开场白',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_personality_id (personality_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟形象表';

-- 聊天会话表
CREATE TABLE IF NOT EXISTS t_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    avatar_id BIGINT NOT NULL COMMENT '形象ID',
    title VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_avatar_id (avatar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role TINYINT NOT NULL COMMENT '角色: 1-用户 2-AI',
    content TEXT NOT NULL COMMENT '消息内容',
    emotion VARCHAR(50) DEFAULT NULL COMMENT '情绪标签',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 用户记忆表
CREATE TABLE IF NOT EXISTS t_user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    avatar_id BIGINT DEFAULT NULL COMMENT '关联形象ID',
    category TINYINT DEFAULT 1 COMMENT '分类: 1-学习 2-偏好 3-成就 4-事件',
    memory_key VARCHAR(100) DEFAULT NULL COMMENT '记忆键',
    value TEXT NOT NULL COMMENT '记忆内容',
    importance FLOAT DEFAULT 0.5 COMMENT '重要性 0-1',
    last_access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记忆表';