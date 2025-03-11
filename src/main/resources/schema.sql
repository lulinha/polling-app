CREATE TABLE IF NOT EXISTS users (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(40) NOT NULL,
    username varchar(15) NOT NULL,
    email varchar(40) NOT NULL,
    password varchar(100) NOT NULL,
    points int DEFAULT 0,
    level int DEFAULT 1,
    banned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS roles (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(60) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS polls (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question varchar(140) NOT NULL,
    expiration_date_time TIMESTAMPTZ NOT NULL,
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by bigint DEFAULT NULL,
    updated_by bigint DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS choices (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text varchar(40) NOT NULL,
    poll_id bigint NOT NULL,
    CONSTRAINT fk_choices_poll_id FOREIGN KEY (poll_id) REFERENCES polls (id)
);

CREATE TABLE IF NOT EXISTS votes (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL,
    poll_id bigint NOT NULL,
    choice_id bigint NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votes_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_votes_poll_id FOREIGN KEY (poll_id) REFERENCES polls (id),
    CONSTRAINT fk_votes_choice_id FOREIGN KEY (choice_id) REFERENCES choices (id)
);

CREATE TABLE IF NOT EXISTS kafka_outbox (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    message_body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    retry_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS kafka_dead_letter (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    message_body TEXT NOT NULL,
    error TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    retry_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS follows (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    follower_id bigint NOT NULL,
    followed_id bigint NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follows_follower_id FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_follows_followed_id FOREIGN KEY (followed_id) REFERENCES users (id),
    CONSTRAINT uk_follows_follower_followed UNIQUE (follower_id, followed_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    message_body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    retry_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS feedback (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL,
    feedback_text text NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    response_text text,
    responded_at TIMESTAMPTZ,
    FOREIGN KEY (user_id) REFERENCES users(id)
);


CREATE TABLE IF NOT EXISTS categories (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS tags (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

-- 关联表
CREATE TABLE IF NOT EXISTS poll_categories (
    poll_id BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (poll_id, category_id)
);

CREATE TABLE IF NOT EXISTS poll_tags (
    poll_id BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (poll_id, tag_id)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_poll_categories_category ON poll_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_poll_tags_tag ON poll_tags(tag_id);
-- 如果使用自动创建标签方案，建议添加唯一性约束：


CREATE TABLE IF NOT EXISTS favorites (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    poll_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束（级联删除）
    CONSTRAINT fk_favorite_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_favorite_poll 
        FOREIGN KEY (poll_id) 
        REFERENCES polls(id) 
        ON DELETE CASCADE,
    
    -- 复合唯一约束（防止重复收藏）
    CONSTRAINT uniq_user_poll 
        UNIQUE (user_id, poll_id)
);

-- 索引优化（按用户查询收藏列表）
CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites USING BRIN (user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_created ON favorites USING BRIN (created_at);

-- 可选：为高频访问字段添加覆盖索引
CREATE INDEX IF NOT EXISTS idx_favorites_user_poll 
    ON favorites (user_id, poll_id) 
    INCLUDE (created_at);


-- 评论表
-- 创建评论表
CREATE TABLE IF NOT EXISTS comments (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,                -- 评论唯一ID（自增主键）
    content TEXT NOT NULL,                      -- 评论内容（支持长文本）
    like_count INT DEFAULT 0,
    poll_id BIGINT NOT NULL,                    -- 关联的投票ID
    user_id BIGINT,                             -- 评论用户ID（允许匿名时可为NULL）
    parent_id BIGINT,                           -- 父评论ID（支持嵌套结构）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间（带时区）
    updated_at TIMESTAMPTZ,                     -- 最后修改时间
    
    -- 外键约束（级联删除策略）
    CONSTRAINT fk_comment_poll 
        FOREIGN KEY (poll_id) 
        REFERENCES polls(id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_comment_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL,  -- 用户删除后保留评论但置空
        
    CONSTRAINT fk_comment_parent 
        FOREIGN KEY (parent_id) 
        REFERENCES comments(id) 
        ON DELETE CASCADE,   -- 父评论删除时级联删除子评论
        
    -- 内容长度约束
    CONSTRAINT check_content_length 
        CHECK (LENGTH(content) BETWEEN 1 AND 2000)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_comments_poll ON comments(poll_id);       -- 按投票查询优化
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments(parent_id);   -- 嵌套查询优化
CREATE INDEX IF NOT EXISTS idx_comments_created ON comments USING BRIN (created_at); -- 时间范围查询优化

-- 可选：添加全文搜索支持
CREATE INDEX IF NOT EXISTS idx_comments_content_search ON comments 
    USING GIN (to_tsvector('english', content));

-- 创建评论点赞表
CREATE TABLE IF NOT EXISTS comment_likes (
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    PRIMARY KEY (user_id, comment_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- 添加索引优化
CREATE INDEX IF NOT EXISTS idx_comment_likes_comment ON comment_likes (comment_id);


CREATE TABLE IF NOT EXISTS notifications (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,   
    type VARCHAR(20) NOT NULL,
    recipient_id BIGINT NOT NULL,
    source_id BIGINT,
    content VARCHAR(500),
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外键约束
    CONSTRAINT fk_notification_user
        FOREIGN KEY (recipient_id) 
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 复合索引
    CONSTRAINT uc_notification_unique
        UNIQUE (type, recipient_id, source_id)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(recipient_id) WHERE read = false;
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications USING BRIN (created_at);

-- 创建 mentions 表
CREATE TABLE IF NOT EXISTS mentions (
    id BIGSERIAL PRIMARY KEY,                 -- 自增主键（对应实体类Long id）
    read BOOLEAN NOT NULL DEFAULT false,   -- 是否已读（对应boolean isRead）
    notification_id BIGINT NOT NULL,          -- 关联通知ID（对应Long notificationId）
    comment_id BIGINT NOT NULL,               -- 关联评论ID
    mentioned_user_id BIGINT NOT NULL,        -- 被提及用户ID
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    
    -- 外键约束
    CONSTRAINT fk_mention_comment 
        FOREIGN KEY (comment_id) 
        REFERENCES comments(id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_mention_user 
        FOREIGN KEY (mentioned_user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_mention_notification 
        FOREIGN KEY (notification_id) 
        REFERENCES notifications(id) 
        ON DELETE CASCADE,
        
    -- 唯一性约束（可选：防止重复提及）
    CONSTRAINT uc_comment_mention 
        UNIQUE (comment_id, mentioned_user_id)
);