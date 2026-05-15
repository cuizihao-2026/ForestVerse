-- ============================================
-- RBAC初始数据
-- ============================================

-- 插入权限数据
INSERT INTO permission (name, resource, action, description) VALUES
-- 文章权限
('article:create', 'article', 'create', '编写文章'),
-- 评论权限
('comment:use', 'comment', 'use', '使用评论功能'),
-- AI助读权限
('ai:assist', 'ai', 'assist', '使用AI助读功能'),
-- 用户管理
('user:manage', 'user', 'manage', '用户管理'),
-- 角色管理
('role:manage', 'role', 'manage', '角色管理'),
-- 内容管理
('article:manage', 'article', 'manage', '内容管理'),
-- 附件管理
('file:manage', 'file', 'manage', '附件管理'),
-- 审核管理
('review:manage', 'review', 'manage', '审核管理'),
-- 网站设置管理
('site:manage', 'site', 'manage', '网站设置管理')
-- 分类管理
('article.class', 'article', 'class', '分类管理')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 插入角色权限数据（超级管理员放首位）
INSERT INTO role_permission (role_name, permission_names, description) VALUES
-- 1. 超级管理员
('SUPER_ADMIN', 'article:create,comment:use,ai:assist,user:manage,role:manage,article:manage,article.class,file:manage,review:manage,site:manage', '超级管理员'),
-- 2. 管理员
('ADMIN', 'article:create,comment:use,ai:assist,user:manage,article:manage,article.class,file:manage,review:manage,site:manage', '管理员'),
-- 3. 作者
('AUTHOR', 'article:create,comment:use', '作者'),
-- 4. 普通用户
('USER', 'comment:use', '普通用户')
ON DUPLICATE KEY UPDATE permission_names = VALUES(permission_names), description = VALUES(description);
