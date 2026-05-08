# 森域社区 (ForestVerse) v1.3.0

一个功能完整的现代化社区交流平台，支持文章发布、评论互动、即时聊天、好友社交、AI辅助等功能。

---

## 📋 目录

- [项目简介](#-项目简介)
- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [项目结构](#-项目结构)
- [配置说明](#-配置说明)
- [更新日志](#-更新日志)

---

## 🌟 项目简介

森域社区 (ForestVerse) 是一个全栈 Web 应用程序，专为社区交流设计。采用前后端分离架构，提供丰富的社区功能。

### 核心特点

- 🚀 **高性能**：Java 21 + 虚拟线程优化，显著提升并发处理能力
- 🔐 **安全可靠**：全新RBAC权限管理、XSS防护、SSL加密
- 🤖 **AI赋能**：AI文章助读、智能审核、流式输出
- 💾 **数据安全**：完整备份中心，支持手动/自动备份、配置文件
- 📱 **移动端优化**：全面优化的移动端适配，更好的移动设备体验
- 🎨 **全新界面**：全面重构的界面布局和交互体验
- ⚡ **实时通信**：WebSocket即时聊天、实时通知

---

## ✨ 功能特性

### 用户系统
- 用户注册、登录、退出
- 找回密码（邮件验证）
- 个人资料编辑、头像上传
- 账号密码修改
- 在线状态显示

### 文章系统
- 文章发布（富文本编辑器）
- 文章编辑、删除、下架
- 文章列表浏览（无限滚动分页）
- 文章详情查看
- 文章点赞
- 文章审核（人工/AI）
- 文章封面缓存

### 评论系统
- 发表评论、回复评论
- 评论列表查看
- 评论审核（人工/AI）
- 评论实时通知
- 评论删除

### 好友系统
- 发送好友请求
- 接收/拒绝好友请求
- 好友列表管理
- 好友分组
- 好友备注

### 聊天功能
- 一对一即时聊天
- 发送文本消息
- 发送图片消息
- 消息已读状态
- 聊天记录查询
- 在线状态显示
- 过期图片自动清理，显示默认占位图

### AI功能
- AI文章助读（流式输出）
- AI文章审核
- AI评论审核
- AI审核配置

### 管理后台
- **角色管理**：全新RBAC权限管理系统，支持新建角色、权限分配、角色列表、编辑、删除
- **备份中心**：手动备份、自动备份、下载、恢复、删除、上传、保留策略、配置文件
- **用户管理**：用户列表、状态修改、角色设置
- **内容管理**：文章审核、评论审核
- **附件中心**：文件列表、删除、分页查询
- **系统设置**：网站配置、邮件配置、AI配置、备份设置
- **数据统计**：用户统计、文章统计、资源统计
- **在线用户**：实时在线用户列表
- **强制刷新**：管理员可强制刷新所有用户页面
- **文章下架广播**：文章下架时自动通知用户

### 系统功能
- WebSocket实时通信
- 定时任务（聊天图片自动清理、自动备份）
- 文件上传（头像、文章封面、聊天图片）
- 动态配置（后台修改实时生效）
- 人机验证码
- 邮箱验证码
- Caffeine本地缓存（文章内容7天缓存）
- XSS防护

---

## 🛠️ 技术栈

### 后端
- **框架**：Spring Boot 4.0.6
- **数据库**：MySQL 8.0
- **持久层**：MyBatis
- **认证**：JWT（支持环境变量配置密钥）
- **权限管理**：RBAC（基于角色的访问控制）
- **实时通信**：WebSocket
- **缓存**：Caffeine（本地缓存）
- **构建工具**：Maven
- **JDK版本**：21+
- **虚拟线程**：优化人机验证、AI审核、AI助读等场景

### 前端
- **框架**：Vue 3.5.32
- **语言**：TypeScript 6.0.2
- **UI组件库**：Element Plus 2.13.7
- **路由**：Vue Router 4.6.4
- **状态管理**：Vue 3 Composition API (ref/reactive)
- **富文本编辑器**：Tiptap 3.22.5
- **构建工具**：Vite 8.0.4

### 其他
- **AI集成**：支持DeepSeek等大语言模型
- **邮件服务**：JavaMail
- **定时任务**：Spring Scheduled
- **文件上传**：Spring Boot Multipart

---

## 📋 环境要求

### 后端环境
- **JDK**：21 或更高版本
- **Maven**：3.6 或更高版本
- **MySQL**：8.0 或更高版本

### 前端环境
- **Node.js**：18 或更高版本
- **npm**：9 或更高版本（或使用 yarn/pnpm）

### 开发工具（可选）
- **IDE**：IntelliJ IDEA / VS Code
- **数据库管理**：Navicat / DBeaver / MySQL Workbench

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd ForestVerse
```

### 2. 数据库准备

创建数据库并导入表结构：

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS forestverse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出MySQL
EXIT;

# 导入表结构
mysql -u root -p forestverse < backend/src/main/resources/db/migration/senyu.sql
```

### 3. 配置环境变量

推荐使用环境变量配置敏感信息：

**Windows PowerShell**：
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_jwt_secret_key"
```

**Linux/Mac**：
```bash
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export JWT_SECRET="your_jwt_secret_key"
```

### 4. 启动后端

```bash
cd backend

# 使用Maven编译并运行
./mvnw spring-boot:run
# Windows用户使用
mvnw.cmd spring-boot:run
```

后端服务将在 `http://localhost:8010` 启动

### 5. 启动前端

新开一个终端窗口：

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:8020` 启动

### 6. 访问应用

打开浏览器访问：`http://localhost:8020`

**默认管理员账号**（需要在数据库中手动创建或通过注册后修改角色）：
- 用户名：admin
- 密码：admin123

---

## 📂 项目结构

```
ForestVerse
├── backend/                    # 后端源码
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/backend/
│   │   │   │   ├── config/    # 配置类（安全、WebSocket、邮件等）
│   │   │   │   ├── controller/# 控制器（API接口）
│   │   │   │   ├── dto/       # 数据传输对象
│   │   │   │   ├── entity/    # 数据库实体
│   │   │   │   ├── mapper/    # MyBatis映射
│   │   │   │   ├── scheduler/ # 定时任务
│   │   │   │   ├── service/   # 业务逻辑层
│   │   │   │   └── utils/     # 工具类
│   │   │   └── resources/
│   │   │       ├── config/    # 网站配置
│   │   │       ├── db/        # 数据库脚本
│   │   │       └── mappers/   # MyBatis XML映射
│   ├── config/                # 外部配置目录
│   └── pom.xml                # Maven配置
│
└── frontend/                  # 前端源码
    ├── src/
    │   ├── components/        # Vue组件
    │   │   ├── admin/         # 管理后台组件
    │   │   ├── article/       # 文章相关组件
    │   │   ├── common/        # 通用组件
    │   │   ├── friends/       # 好友聊天组件
    │   │   ├── home/          # 首页组件
    │   │   ├── login/         # 登录组件
    │   │   ├── personal/      # 个人中心组件
    │   │   └── register/      # 注册组件
    │   ├── router/            # 路由配置
    │   ├── stores/            # 状态管理
    │   ├── styles/            # 样式文件
    │   ├── utils/             # 工具函数
    │   ├── views/             # 页面视图
    │   ├── App.vue            # 根组件
    │   └── main.ts            # 入口文件
    ├── package.json           # 依赖配置
    ├── vite.config.ts         # Vite配置
    └── tsconfig.json          # TypeScript配置
```

---

## ⚙️ 配置说明

### 后端配置

**文件位置**：`backend/src/main/resources/application.yaml`

#### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/forestverse?useSSL=true&requireSSL=true&serverTimezone=UTC
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:your_password_here}
```

#### JWT配置
```yaml
jwt:
  secret: ${JWT_SECRET:your_default_secret_key}
  expiration: 86400000
```

### 网站配置

**文件位置**：`backend/config/website-settings.yaml`

这个文件包含网站的核心配置，包括：
- 心跳检测配置
- 邮件服务配置
- 验证码配置
- 审核配置
- AI服务配置
- 备份配置

**重要**：大部分配置可以在管理后台的"系统设置"中直接修改，无需编辑配置文件！

---

## 📖 更新日志

详细的版本更新记录请查看 [CHANGELOG.md](./CHANGELOG.md)

### v1.3.0 (2026-05-09) 主要更新
- ✨ 全新RBAC权限管理系统重构
- ✨ 界面全面重构，优化交互体验
- ✨ 大幅优化移动端适配
- ✨ 聊天图片处理优化
- 🐛 修复过期聊天图片加载异常
- 🐛 修复用户注册问题

### v1.2.0 (2026-05-05) 主要更新
- 🛡️ 安全增强：XSS防护、JWT密钥环境变量配置、SSL强制连接
- ✨ 功能优化：备份中心动态配置、统一任务调度器
- 🐛 问题修复：缓存刷新问题、线程池拒绝策略

---

## 📝 开发指南

### 代码规范
- 后端遵循阿里巴巴Java开发规范
- 前端遵循Vue官方风格指南
- 使用TypeScript保证类型安全

### 开发流程
1. 创建功能分支
2. 进行代码开发
3. 提交代码（使用规范的commit message）
4. 提交Pull Request

---

## 📄 许可证

本项目使用 MIT 许可证，详情请查看 [LICENSE](./LICENSE)

---

## 🙏 致谢

感谢所有为本项目做出贡献的开发者！

---

**版本**：v1.3.0  
**更新日期**：2026-05-09
