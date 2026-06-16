# 🛒 超市进销存管理系统 (SuperMarket Management System)

> 基于 Vue 3 + Spring Boot + MyBatis + MySQL 的全栈 Web 应用  
> 内置 DeepSeek AI 猫智能体，支持三级管理员分权管理、国际化电话验证、Excel 批量导入导出

[![Tech Stack](https://img.shields.io/badge/Vue-3.5-42b883?logo=vue.js)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6db33f?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1?logo=mysql)](https://www.mysql.com/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-409eff)](https://element-plus.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-536dfe)](https://deepseek.com/)

---

## 📖 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [功能矩阵](#功能矩阵)
- [项目结构](#项目结构)
- [请求流程](#请求流程)
- [快速开始](#快速开始)
- [部署指南](#部署指南)
- [默认账号](#默认账号)
- [配置说明](#配置说明)
- [相关文档](#相关文档)
- [日志记录](#日志记录)
- [已知问题](#已知问题)

---

## 项目概述

超市进销存管理系统是一个基于 B/S 架构的 Web 应用，旨在帮助超市管理人员高效管理商品进货、库存及相关业务数据。系统支持三级管理员和普通用户两种角色，覆盖供应商管理、商品管理、员工管理、采购管理（主表-明细双表模式）、会员管理五大核心业务模块。

**核心亮点**：
- 🤖 **DeepSeek AI 智能体**：悬浮小白猫，内置 40 条系统知识库，支持自然语言问答
- 👑 **三级管理员分权**：一号(11)/二号(10)/三号(01) 管理员权限精确分离
- 🌍 **国际化电话验证**：支持 29 个国家/地区的电话格式验证
- 📊 **Excel 批量操作**：全模块支持 Excel 导入/导出（Apache POI）
- 🎨 **现代化 UI**：Vue 3 + Element Plus，渐变背景 + 粒子动效
- 📱 **响应式布局**：适配 PC 端和移动端

---

## 技术栈

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **后端框架** | Spring Boot | 4.0.6 | Java 21 |
| **ORM** | MyBatis (Spring Boot Starter) | 4.0.1 | XML SQL 映射 |
| **数据库** | MySQL | 8.0.44 | InnoDB, utf8mb4 |
| **连接池** | HikariCP | (内置) | 最大 20 连接 |
| **缓存** | Caffeine | (内置) | 本地缓存, 500 条/10 分钟 |
| **Excel** | Apache POI | 5.2.5 | .xlsx 读写 |
| **AI** | DeepSeek API | deepseek-chat | 智能体对话 |
| **构建工具** | Maven | 3.9+ | 后端构建 |
| **前端框架** | Vue 3 | 3.5.34 | Composition API |
| **UI 库** | Element Plus | 2.14.1 | 企业级组件 |
| **构建工具** | Vite | 8.0.12 | 前端构建 |
| **HTTP** | Axios | — | API 调用 |

---

## 功能矩阵

| 模块 | 一号管理员 (11xxx) | 二号管理员 (10xxx) | 三号管理员 (01xxx) | 普通用户 |
|------|:---:|:---:|:---:|:---:|
| **供应商管理** | ✅ 增删改查+导入导出 | ✅ 增删改查+导入导出 | 👁️ 仅查看+导出 | ❌ |
| **商品管理** | ✅ 增删改查+导入导出 | ✅ 增删改查+导入导出 | 👁️ 仅查看+导出 | 👁️ 仅查看+导出 |
| **员工管理** | ✅ 增删改查+导入导出 | 👁️ 仅查看+导出 | ✅ 增删改查+导入导出 | ❌ |
| **采购管理** | ✅ 增删改查+导入导出 | 👁️ 仅查看+导出 | ✅ 增删改查+导入导出 | 👁️ 仅查看+导出 |
| **会员管理** | ✅ 增删改查+导入导出 | ✅ 增删改查+导入导出 | ✅ 增删改查+导入导出 | ❌ |
| **会员等级修改** | ✅ 除自己外所有 | ✅ 除11/自己和01 | ✅ 除11/10和自己 | ❌ |
| **个人信息** | ✅ 头像/资料/密码 | ✅ 头像/资料/密码 | ✅ 头像/资料/密码 | ✅ 头像/资料/密码 |
| **AI 智能体** | ✅ | ✅ | ✅ | ✅ |

**关键业务规则**：
- 系统最多 3 个管理员，超出不可注册
- 管理员注册后自动加入会员表（一号→SVIP，二/三号→VIP）
- 普通员工需管理员在员工表中预录入信息后才能注册（账号 9 位，00 开头）
- 密码至少 8 位，含大小写字母+数字+特殊字符，全局不可重复
- 电话同区号下不可重复；跨区号可重复
- 年龄限制 18~66 岁
- 采购主表无对应清单号时，明细表不可新增/导入

---

## 项目结构

```
Supermarket-Management-System/
├── README.md                     # 📖 项目说明与部署文档（本文件）
├── 数据库设计报告.md               # 🗄️ 数据库设计文档（6 表 + 索引 + 迁移）
├── 项目设计说明书.md               # 📐 项目设计详细说明（架构/API/权限/采购）
├── 课设.md                       # 📋 课程设计需求文档
├── 配置.md                       # ⚙️ 技能配置说明
│
├── database/                     # 🗄️ 数据库脚本
│   ├── _localhost-2026_06_04_20_24_31-dump.sql   # 全量建表+初始数据
│   └── migration_add_member_level.sql           # 迁移脚本（sys_user 会员等级）
│
├── backend/                      # ☕ Spring Boot 后端
│   ├── pom.xml                   # Maven 配置
│   └── src/main/
│       ├── java/com/supermarket/backend/
│       │   ├── BackendApplication.java          # 🚀 启动类
│       │   ├── common/Result.java               # 📦 统一响应封装
│       │   ├── config/CorsConfig.java           # 🔧 CORS 配置
│       │   ├── controller/                      # 🎮 控制器层 (7 个)
│       │   │   ├── AgentController.java         #   🤖 AI 智能体
│       │   │   ├── GoodsController.java         #   📦 商品管理
│       │   │   ├── MemberController.java        #   👤 会员管理
│       │   │   ├── PurchaseDetailController.java #  📋 采购明细
│       │   │   ├── PurchaseMainController.java   #  📋 采购主表
│       │   │   ├── SupplierController.java       #  🏭 供应商管理
│       │   │   └── SysUserController.java        #  👥 用户管理
│       │   ├── entity/                          # 📄 实体类 (7 个)
│       │   ├── mapper/                          # 🗺️ Mapper 接口 (6 个)
│       │   ├── service/                         # ⚙️ 业务层 (7 个)
│       │   └── util/ExcelUtil.java              # 📊 Excel 读写工具
│       └── resources/
│           ├── application.yml                  # ⚙️ 应用配置
│           └── mapper/                          # 📝 MyBatis XML (6 个)
│
├── frontend/                     # 🎨 Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html                # 入口 HTML（含 favicon）
│   └── src/
│       ├── main.js               # 🔌 入口文件
│       ├── App.vue               # 🌐 根组件
│       ├── style.css             # 🎨 全局样式
│       ├── router/index.js       # 🧭 路由配置
│       ├── utils/request.js      # 🌐 Axios 封装
│       ├── api/                  # 📡 API 模块 (7 个)
│       │   ├── agent.js          #   🤖 AI 智能体
│       │   ├── goods.js          #   📦 商品 API
│       │   ├── member.js         #   👤 会员 API
│       │   ├── purchaseDetail.js #   📋 采购明细 API
│       │   ├── purchaseMain.js   #   📋 采购主表 API
│       │   ├── supplier.js       #   🏭 供应商 API
│       │   └── user.js           #   👥 用户 API
│       └── views/                # 📄 页面组件 (8 个)
│           ├── Login.vue         #   🔐 登录注册（视频+表单+密码强度）
│           ├── Layout.vue        #   🏠 主布局（侧边栏+导航+头像）
│           ├── UserInfo.vue      #   👤 个人信息
│           ├── Employee.vue      #   👥 员工管理
│           ├── Goods.vue         #   📦 商品管理
│           ├── Purchase.vue      #   📋 采购管理（双表）
│           ├── Supplier.vue      #   🏭 供应商管理
│           └── Member.vue        #   👤 会员管理
│
└── log/                          # 📝 开发日志
    ├── Day1_2026-06-01.md ~ Day7_2026-06-15.md
    └── images/                   # 🖼️ 截图
```

---

## 请求流程

```
1. 前端/ApiFox 发送 HTTP 请求 (Axios)

   ↓

2. 进入 Controller (@RestController)
   接收参数 → 调用 Service

   ↓

3. Service 处理业务逻辑
   校验 → 转换 → 调用 Mapper

   ↓

4. Mapper 接口 → XML SQL 映射

   ↓

5. JDBC → MySQL 数据库 (supermarket_db)

   ↓

6. 数据原路返回 → Result<T> 统一格式响应
   { "code": 200, "msg": "操作成功", "data": [...] }
```

---

## 快速开始

### 环境要求

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21 | Java 开发环境 |
| MySQL | 8.0 | 数据库服务（需启动） |
| Maven | 3.9 | 后端构建 |
| Node.js | 18+ | 前端构建（含 npm） |

### 1️⃣ 克隆项目

```bash
git clone <repo-url>
cd Supermarket-Management-System
```

### 2️⃣ 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行建库建表脚本
source database/_localhost-2026_06_04_20_24_31-dump.sql

# 执行迁移脚本（如需）
source database/migration_add_member_level.sql
```

### 3️⃣ 启动后端

```bash
cd backend

# 确认 application.yml 中数据库密码正确
# spring.datasource.password=你的MySQL密码

mvn spring-boot:run
# 或: mvn clean package -DskipTests && java -jar target/backend-0.0.1-SNAPSHOT.jar
```

后端启动后运行在 `http://localhost:8080`

### 4️⃣ 启动前端

```bash
cd frontend

npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`

### 5️⃣ 访问系统

打开浏览器访问 `http://localhost:5173`，使用默认账号登录。

---

## 部署指南

### 本地开发部署

参见 [快速开始](#快速开始)，前后端同时启动即可。

### 生产部署（本地网络）

```bash
# 后端: 构建 JAR 包并运行
cd backend
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar

# 前端: 构建静态文件
cd frontend
npm run build
# 产物在 frontend/dist/ 目录，可用 Nginx 部署

# 将 dist/ 复制到 Nginx 目录
# Nginx 配置反向代理 /api → http://localhost:8080
```

### 外网访问（Cpolar 隧道）

```bash
# 安装 cpolar 后
cpolar http 8080   # 暴露后端
cpolar http 5173   # 暴露前端
```

> ⚠️ 通过 Cpolar 访问时响应较慢，建议本地开发使用 `localhost`。

### Docker 部署（推荐，需自行编写 Dockerfile）

```dockerfile
# 后端 Dockerfile 示例
FROM eclipse-temurin:21-jre
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 默认账号

| 用户名 | 密码 | 角色 | 会员等级 | 权限范围 |
|--------|------|------|:---:|------|
| `110001` | `Sh123456@` | 一号管理员 | SVIP | 全部管理权限 |
| `100001` | `Sh1234567@` | 二号管理员 | VIP | 供应商、商品、会员 |
| `010001` | `Sh1234567!` | 三号管理员 | VIP | 员工、采购、会员 |

> 首次登录后建议修改密码。普通员工需管理员先在员工表中录入信息后才能注册。

---

## 配置说明

### 后端核心配置 (`backend/src/main/resources/application.yml`)

```yaml
server:
  port: 8080                          # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/supermarket_db?...   # 数据库连接
    username: root                     # 数据库用户名
    password: Sh241612                 # ⚠️ 修改为你的密码
    hikari:
      maximum-pool-size: 20            # 连接池大小

  cache:
    type: caffeine                     # 本地缓存
    caffeine:
      spec: maximumSize=500,expireAfterWrite=600s

agent:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:sk-...}  # ⚠️ 设置为你的 DeepSeek API Key
    model: deepseek-chat
```

### 前端配置 (`frontend/vite.config.js`)

前端开发服务器默认代理 `/api` 到 `http://localhost:8080`，无需额外配置。

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | `sk-your-deepseek-api-key` |
| `REDIS_HOST` | Redis 主机地址（可选） | `localhost` |
| `REDIS_PORT` | Redis 端口（可选） | `6379` |

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [数据库设计报告.md](./数据库设计报告.md) | 数据库表结构、索引、外键、迁移脚本完整文档 |
| [项目设计说明书.md](./项目设计说明书.md) | 系统架构、API 接口、权限模型、采购模块、AI 智能体详细设计 |
| [课设.md](./课设.md) | 课程设计原始需求文档 |
| [配置.md](./配置.md) | 技能(Skills)配置说明 |
| [log/](./log/) | 开发日志（Day1 ~ Day7） |

---

## 日志记录

| 日期 | 日志 | 主要内容 |
|------|------|----------|
| 06-01 | [Day1](log/Day1_2026-06-01.md) | 项目初始化、数据库建表、后端框架搭建 |
| 06-04 | [Day2](log/Day2_2026-06-04.md) | 前端框架搭建、登录注册页面、Element Plus 集成 |
| 06-05 | [Day3](log/Day3_2026-06-05.md) | 采购管理双表、Excel 导入导出、三级权限 |
| 06-09 | [Day4](log/Day4_2026-06-09.md) | 会员等级、头像上传、个人资料编辑 |
| 06-11 | [Day5](log/Day5_2026-06-11.md) | AI 猫智能体、DeepSeek 集成、知识库设计 |
| 06-12 | [Day6](log/Day6_2026-06-12.md) | Favicon 更换、手机号校验修复、采购管理全面修复、分页搜索 |
| 06-15 | [Day7](log/Day7_2026-06-15.md) | 课设报告完成、E-R图、Bug修复（会员时间/商品编号/Excel导入导出） |

---

## 已知问题与改进方向

### ✅ 已实现（原已知问题中已修复）

| 特性 | 实现方式 | 相关文件 |
|------|----------|----------|
| BCrypt 密码加密 | `BCryptPasswordEncoder`，支持旧明文密码自动升级 | `PasswordEncoderConfig.java`, `SysUserService.java` |
| Redis 缓存 + Redisson 分布式锁 | 二级缓存(Caffeine+Redis)，Redisson 锁保护管理员注册 | `RedisConfig.java`, `RedissonConfig.java` |
| HikariCP 连接池 | 最大 20 连接，最小 5 空闲 | `application.yml` |
| Caffeine 本地缓存 | 500 条 / 10 分钟过期 | `application.yml` |
| Gzip 响应压缩 | JSON/JS/CSS 文本压缩 | `application.yml` |
| 异步线程池 | 核心 8 线程 / 最大 16 线程 | `ThreadPoolConfig.java` |

### ⚠️ 需外部依赖（代码已就绪，需启动对应服务）

| 特性 | 状态 | 说明 |
|------|:---:|------|
| Redis/Redisson | 🔶 待启用 | 代码已完整实现，需安装 Redis 服务并在 `application.yml` 中配置 `spring.data.redis.host` 后自动激活 |
| AI 智能体 | 🔶 待配置 | 代码已完整实现，需配置环境变量 `DEEPSEEK_API_KEY` |

#### 启动 Redis + Redisson 分布式锁

**1. 安装 Redis（Windows）**

```powershell
# 方式一：使用 winget
winget install Redis.Redis

# 方式二：使用 Scoop
scoop install redis

# 方式三：Docker（推荐，无需安装到系统）
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

**2. 启动 Redis**

```powershell
# winget/Scoop 安装的：新开终端运行
redis-server

# Docker 安装的：
docker start redis
```

**3. 配置 `application.yml`**

```yaml
# backend/src/main/resources/application.yml
spring:
  data:
    redis:
      host: localhost     # ⬅️ 取消注释这行即可激活
      port: 6379          # ⬅️ 取消注释这行即可激活
```

> `RedissonConfig` 通过 `@ConditionalOnProperty(name = "spring.data.redis.host")` 控制——只要配置了 `host`，启动时自动创建 RedissonClient 和二级缓存；不配则跳过，使用纯 Caffeine 本地缓存。

**4. 重启后端**：`mvn spring-boot:run`

**验证**：启动日志中看到 `Redisson` 和 `RedisCacheManager` 相关输出即表示激活成功。此时管理员注册会使用分布式锁防止并发超限。

---

#### 配置 DeepSeek AI 智能体

**1. 获取 API Key**

访问 [platform.deepseek.com](https://platform.deepseek.com/) 注册并创建 API Key。

**2. 配置环境变量**

```powershell
# PowerShell — 当前终端生效
$env:DEEPSEEK_API_KEY = "sk-你的key"

# 或永久设置（所有终端生效）
[System.Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', 'sk-你的key', 'User')
```

```bash
# Git Bash / WSL — 当前终端生效
export DEEPSEEK_API_KEY=sk-你的key

# 永久设置（追加到 ~/.bashrc 或 ~/.zshrc）
echo 'export DEEPSEEK_API_KEY=sk-你的key' >> ~/.bashrc
```

**3. 重启后端**：`mvn spring-boot:run`

**验证**：登录系统后，右下角出现白色小猫悬浮按钮，点击可对话。

### 🔧 待实现

| 问题 | 优先级 | 说明 |
|------|:---:|------|
| 无 JWT/Token 服务端认证 | 🔴 高 | 当前基于 localStorage 角色字段判断权限，前端可被绕过 |
| HTTP 明文传输 | 🟡 中 | 生产环境需配置 SSL 证书启用 HTTPS |
| 无操作审计日志 | 🟡 中 | 关键操作（删改、导入）未记录日志 |
| Cpolar 外网访问延迟高 | 🟡 中 | 隧道转发每请求增加 500ms~2s；建议生产部署使用直连或云服务器 |
| 明细商品选择为文本输入 | 🟢 低 | 采购明细编辑时 goodsId 需手动输入，建议改为 `<el-select>` 下拉选择 |
| 无主表-明细联动筛选 | 🟢 低 | 点击主表行后明细表不自动过滤关联记录 |
| 无 Swagger/Knife4j API 文档 | 🟢 低 | 前后端联调缺少可视化接口文档 |
| 销售/库存模块缺失 | 🔵 规划 | 当前仅覆盖采购端，进销存完整闭环需补充销售管理、库存管理 |

---

## License

Educational project — Supermarket Management System © 2026 Sun Hao
