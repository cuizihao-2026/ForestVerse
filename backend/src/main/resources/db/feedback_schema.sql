-- ========================================
-- 反馈系统数据库表设计（简化版）
-- ========================================

-- 1. 反馈主表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    type VARCHAR(20) NOT NULL COMMENT '反馈类型：BUG/FEATURE/CONTENT/OTHER',
    title VARCHAR(200) NOT NULL COMMENT '反馈标题',
    content TEXT NOT NULL COMMENT '反馈详细内容',
    contact VARCHAR(100) COMMENT '联系方式',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW/NORMAL/HIGH',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/CLOSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈主表';

-- 2. 反馈对话表（管理员与用户沟通）
CREATE TABLE IF NOT EXISTS feedback_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL COMMENT '反馈ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    is_admin TINYINT NOT NULL DEFAULT 0 COMMENT '是否管理员：0-用户，1-管理员',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback (feedback_id),
    INDEX idx_feedback_created (feedback_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈对话表';
