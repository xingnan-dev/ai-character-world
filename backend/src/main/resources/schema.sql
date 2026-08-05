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
    source_avatar_id BIGINT DEFAULT NULL COMMENT '来源形象ID(AI生成时关联)',
    is_ai_generated TINYINT DEFAULT 0 COMMENT '是否AI生成: 0-否 1-是',
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
    gender TINYINT DEFAULT 2 COMMENT '性别: 1-男 2-女 3-其他',
    base_model VARCHAR(100) DEFAULT NULL COMMENT '基础模型名称',
    model_url VARCHAR(500) DEFAULT NULL COMMENT '模型文件URL',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
    personality_id BIGINT DEFAULT NULL COMMENT '性格ID',
    slogan VARCHAR(200) DEFAULT NULL COMMENT '标语/开场白',
    source_description VARCHAR(1000) DEFAULT NULL COMMENT '用户原始描述(AI生成的输入)',
    generate_type TINYINT DEFAULT 0 COMMENT '生成方式: 0-手动创建 1-AI生成 2-模板创建',
    generate_result JSON DEFAULT NULL COMMENT 'AI生成完整结果(JSON)',
    template_id BIGINT DEFAULT NULL COMMENT '来源模板ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_personality_id (personality_id),
    INDEX idx_generate_type (generate_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟形象表';

-- 角色属性表(EAV模式，支持细粒度属性扩展)
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

-- VRM资源库表
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

-- 预设角色模板表
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