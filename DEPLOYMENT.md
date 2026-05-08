# 森域社区 (ForestVerse) v1.3.0 - 源码部署教程

本教程将指导您如何从源码部署森域社区。

---

## 📋 目录

- [部署前准备](#-部署前准备)
- [环境安装](#-环境安装)
- [数据库配置](#-数据库配置)
- [后端部署](#-后端部署)
- [前端部署](#-前端部署)
- [Nginx反向代理](#-nginx反向代理)
- [常见问题](#-常见问题)

---

## 🛠️ 部署前准备

### 系统要求

- **操作系统**：Linux (推荐 Ubuntu/CentOS)、Windows、macOS
- **CPU**：2核及以上
- **内存**：4GB及以上
- **磁盘**：至少20GB可用空间

### 端口准备

确保以下端口未被占用：
- `8010`：后端服务
- `8020`：前端开发服务
- `80` / `443`：Nginx（生产环境）

---

## 🔧 环境安装

### 1. 安装 JDK 21

**Ubuntu/Debian**：
```bash
sudo apt update
sudo apt install openjdk-21-jdk -y

# 验证安装
java -version
```

**CentOS/RHEL**：
```bash
sudo yum install java-21-openjdk-devel -y

# 验证安装
java -version
```

**Windows**：
1. 下载 [JDK 21](https://adoptium.net/temurin/releases/?version=21)
2. 安装并配置环境变量
3. 验证：`java -version`

### 2. 安装 MySQL 8.0

**Ubuntu/Debian**：
```bash
sudo apt update
sudo apt install mysql-server -y

# 启动MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 配置MySQL
sudo mysql_secure_installation
```

**CentOS/RHEL**：
```bash
sudo yum install mysql-server -y

# 启动MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 配置MySQL
sudo mysql_secure_installation
```

**Windows**：
1. 下载 [MySQL Installer](https://dev.mysql.com/downloads/installer/)
2. 安装 MySQL Server 8.0

### 3. 安装 Node.js 18+

**Ubuntu/Debian**：
```bash
# 使用NodeSource仓库
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 验证安装
node -v
npm -v
```

**CentOS/RHEL**：
```bash
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo yum install -y nodejs

# 验证安装
node -v
npm -v
```

**Windows**：
1. 下载 [Node.js 18 LTS](https://nodejs.org/)
2. 安装并验证

### 4. 安装 Nginx（生产环境）

**Ubuntu/Debian**：
```bash
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

**CentOS/RHEL**：
```bash
sudo yum install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 🗄️ 数据库配置

### 1. 创建数据库

登录MySQL：
```bash
sudo mysql -u root -p
```

创建数据库和用户：
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS forestverse 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建数据库用户（可选，推荐使用独立用户）
CREATE USER IF NOT EXISTS 'forestverse_user'@'localhost' 
IDENTIFIED BY 'your_strong_password';

-- 授权
GRANT ALL PRIVILEGES ON forestverse.* TO 'forestverse_user'@'localhost';
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

### 2. 导入表结构

```bash
# 克隆项目（如果还没有）
git clone <repository-url>
cd ForestVerse

# 导入表结构
mysql -u root -p forestverse < backend/src/main/resources/db/migration/senyu.sql

# 或者使用刚才创建的用户
mysql -u forestverse_user -p forestverse < backend/src/main/resources/db/migration/senyu.sql
```

### 3. 初始化管理员账号

**方式一：通过注册后修改**

1. 先在前台注册一个普通用户
2. 登录MySQL修改角色：
```sql
USE forestverse;
UPDATE user SET role = 'SUPER_ADMIN' WHERE username = 'your_admin_username';
```

**方式二：直接插入**
```sql
USE forestverse;

-- 插入管理员账号（密码需要加密）
-- 注意：这里的密码需要使用项目中的PasswordUtils加密后的值
-- 初始密码建议通过注册方式设置
```

---

## 🚀 后端部署

### 1. 配置环境变量

创建环境变量配置文件：

**Linux/Mac（创建 .env 文件）**：
```bash
cd backend
nano .env
```

添加以下内容：
```env
# 数据库配置
DB_USERNAME=root
DB_PASSWORD=your_database_password

# JWT密钥（生产环境必须修改为随机字符串）
JWT_SECRET=your_jwt_secret_key_at_least_32_characters_long
```

保存并退出（Ctrl+O, Enter, Ctrl+X）

**Windows PowerShell**：
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_jwt_secret_key_at_least_32_characters_long"
```

或者创建 `set-env.ps1`：
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_jwt_secret_key_at_least_32_characters_long"
```

### 2. 编译后端

```bash
cd backend

# 使用Maven编译
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

编译成功后，会在 `backend/target/` 目录下生成 `backend-0.0.1-SNAPSHOT.jar` 文件

### 3. 运行后端

**开发模式**：
```bash
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

**生产模式（使用JAR包）**：
```bash
# 直接运行
java -jar target/backend-0.0.1-SNAPSHOT.jar

# 指定配置文件运行
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.config.location=file:/path/to/application.yaml
```

### 4. 后台运行（生产环境）

**使用 nohup**：
```bash
cd backend
nohup java -jar target/backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 查看日志
tail -f app.log
```

**使用 systemd（推荐）**：

创建服务文件：
```bash
sudo nano /etc/systemd/system/forestverse.service
```

添加内容：
```ini
[Unit]
Description=ForestVerse Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/path/to/ForestVerse/backend
Environment="DB_USERNAME=root"
Environment="DB_PASSWORD=your_database_password"
Environment="JWT_SECRET=your_jwt_secret_key"
ExecStart=/usr/bin/java -jar /path/to/ForestVerse/backend/target/backend-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
# 重载systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start forestverse

# 设置开机自启
sudo systemctl enable forestverse

# 查看状态
sudo systemctl status forestverse

# 查看日志
sudo journalctl -u forestverse -f
```

### 5. 验证后端

访问：`http://localhost:8010`

或者测试API：
```bash
curl http://localhost:8010/api/test
```

---

## 🎨 前端部署

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 开发模式运行

```bash
npm run dev
```

访问：`http://localhost:8020`

### 3. 生产环境打包

```bash
# 打包
npm run build

# 打包后的文件在 dist/ 目录
ls -la dist/
```

### 4. 使用 nginx 部署前端

将 `dist/` 目录下的文件复制到 nginx 网站目录：

```bash
# Ubuntu/Debian
sudo cp -r frontend/dist/* /var/www/html/

# CentOS/RHEL
sudo cp -r frontend/dist/* /usr/share/nginx/html/
```

---

## 🌐 Nginx 反向代理

### 1. 创建 Nginx 配置

```bash
sudo nano /etc/nginx/sites-available/forestverse
```

添加以下配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 修改为你的域名

    # 前端静态文件
    location / {
        root /var/www/html;  # 修改为前端dist目录的实际路径
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api {
        proxy_pass http://localhost:8010;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket代理
    location /ws {
        proxy_pass http://localhost:8010;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }

    # 上传文件
    location /uploads {
        proxy_pass http://localhost:8010;
        proxy_set_header Host $host;
    }

    # 日志
    access_log /var/log/nginx/forestverse-access.log;
    error_log /var/log/nginx/forestverse-error.log;
}
```

### 2. 启用配置

```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/forestverse /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启nginx
sudo systemctl restart nginx
```

### 3. 配置 HTTPS（推荐）

使用 Let's Encrypt 免费证书：

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx -y  # Ubuntu/Debian
sudo yum install certbot python3-certbot-nginx -y  # CentOS/RHEL

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

Certbot 会自动配置 HTTPS，配置完成后重启 nginx：
```bash
sudo systemctl restart nginx
```

---

## 🔧 常见问题

### 1. 数据库连接失败

**问题**：`Access denied for user 'root'@'localhost'`

**解决**：
- 检查数据库用户名和密码是否正确
- 确认环境变量配置生效
- 检查 MySQL 用户权限

### 2. 端口被占用

**问题**：`Port 8010 was already in use`

**解决**：
```bash
# 查看端口占用
lsof -i :8010  # Linux
netstat -ano | findstr :8010  # Windows

# 杀掉占用进程
kill -9 <PID>  # Linux
taskkill /PID <PID> /F  # Windows
```

或者修改 `application.yaml` 中的端口号

### 3. 前端无法连接后端

**问题**：API 调用失败

**解决**：
- 检查后端服务是否正常运行
- 检查 CORS 配置
- 检查 Nginx 代理配置

### 4. 文件上传失败

**问题**：上传大文件时报错

**解决**：
- 检查 Nginx 配置中的 `client_max_body_size`
- 检查后端 `application.yaml` 中的 `spring.servlet.multipart.max-file-size`

### 5. 备份功能失败

**问题**：无法创建备份

**解决**：
- 确认系统已安装 `mysqldump`
- 检查 `backups/` 目录权限
- 确保数据库用户有锁表权限

### 6. AI 功能不工作

**问题**：AI 助读或审核失败

**解决**：
- 在管理后台检查 AI API 密钥配置
- 确认 AI 服务地址可访问
- 检查网络连接和防火墙设置

---

## 📊 性能优化建议

### 1. JVM 参数优化

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar backend-0.0.1-SNAPSHOT.jar
```

- `-Xms512m`：初始堆内存
- `-Xmx2g`：最大堆内存
- `-XX:+UseG1GC`：使用 G1 垃圾回收器

### 2. MySQL 优化

在 `my.cnf` 或 `my.ini` 中添加：
```ini
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
```

### 3. Nginx 缓存

在 Nginx 配置中添加静态资源缓存：
```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

---

## 🔐 安全检查清单

部署前请确保：

- [ ] 所有敏感信息使用环境变量配置
- [ ] JWT密钥使用强随机字符串（至少32字符）
- [ ] 数据库使用独立用户，权限最小化
- [ ] 启用 HTTPS
- [ ] 配置防火墙，仅开放必要端口
- [ ] 定期更新系统和依赖
- [ ] 设置自动备份
- [ ] 配置日志轮转
- [ ] 禁用不必要的服务

---

## 📞 获取帮助

如遇到问题：

1. 查看日志文件：
   - 后端日志：`backend/app.log`
   - Nginx日志：`/var/log/nginx/`

2. 查看项目 [README.md](./README.md)

3. 查看 [CHANGELOG.md](./CHANGELOG.md)

---

**文档版本**：v1.3.0  
**最后更新**：2026-05-09
