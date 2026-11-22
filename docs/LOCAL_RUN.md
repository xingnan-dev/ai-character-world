# 本地运行与配置

本项目的交付目标是本地使用，不需要公网部署。以下使用 MySQL、普通 Maven 和普通 Vite；不依赖 `.acceptance-run/`、H2、测量中继或自定义启动类。

如希望前端、后端和 MySQL 全部运行在容器中，使用 [Docker Compose 本地运行](DOCKER_LOCAL.md)。两种方式的数据库互相独立；不要把容器卷指向已有 Windows MySQL 数据目录。

## 1. 软件与数据库

本轮实测：Windows 11、JDK 17.0.20.1、Maven 3.9.16、Node.js 24.16.0、npm 11.13.0、MySQL 8.0.34。后端要求 Java 17；仓库没有 Maven Wrapper。其他版本组合未逐一验收。

使用一个明确命名的数据库。首次初始化可在 MySQL 客户端执行：

```sql
CREATE DATABASE companion_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

使用有该库权限的本机 MySQL 账号，端口按实际设置；仓库默认端口是 **3307**，不是 3306。验收或试运行应另建库，不清空日常数据。

Flyway 是唯一 schema 来源，当前迁移 V1–V27。启动会自动校验并执行待迁移项；同版本重启显示 `Schema ... is up to date`。推荐显式设置 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false`，防止将未知的非空库直接当作已有基线。不要修改已执行迁移，不要把空库初始化通过解释为历史库升级通过。接入已有库前先备份并检查迁移历史。

**缺少 AI 配置导致启动失败，并不意味着数据库尚未迁移。** 本轮实际观察到 Flyway 先完成空库迁移，之后才因缺少 `AI_API_KEY` 退出。首次检查务必使用独立库。

## 2. 配置来源与优先级

[`.env.example`](../.env.example) 是不含秘密的键名清单，**不会自动加载**。后端读取 Spring 配置和启动进程环境。

在本指南使用的配置源中，通常顺序为：命令行参数、JVM 系统属性、进程环境变量、外部 YAML、classpath YAML；同一位置的 profile 文件覆盖基础文件。不要同时留下互相矛盾的 `-D` 参数和环境变量。Maven 插件参数也可能改变活动 profile。

- `application.yml` 默认 profile 为 `dev`；它可能加载本机被忽略的 `application-dev.yml`。
- 推荐普通运行显式设置 `SPRING_PROFILES_ACTIVE=local-real`。此名称不要求存在额外的 profile 文件，作用是避免加载 `dev` 的本机覆盖配置；仍使用基础 YAML 和进程环境变量。
- 真实 AI 使用 `AI_*`；`LLM_API_KEY`、`LLM_MODEL`、`LLM_BASE_URL` **不是本应用自动识别的名称**。已有这些变量时，须由自己在启动进程中明确映射；完整 endpoint 还需包含 `/chat/completions`。
- 不提交本地配置。`application-dev.yml`、`.env`、`application-local.yml` 等已被忽略；不要把真实值粘贴到公共示例、日志或 Git。

| 配置 | 用途 |
| --- | --- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 地址与认证；URL 指向自己准备的库 |
| `JWT_SECRET` | 至少 32 个 UTF-8 字节；要跨重启保留登录，需安全保留相同值 |
| `AI_PROVIDER=openai-compatible` | 当前真实 LLM 协议 |
| `AI_MOCK_ENABLED=false` | 真实模式；不能只设置 provider 而忽略该开关 |
| `AI_API_URL` | 完整 Chat Completions URL，不是仅 `/v1` 的 base URL |
| `AI_API_KEY` / `AI_MODEL_NAME` | 有额度和模型权限的配置；空值会明确失败 |
| `AI_MAX_TOKENS` | 单次聊天默认值；记忆、关系、成长、Agent Decision 有自己的调用与上限 |
| `ZHIPU_IMAGE_API_KEY` | 可选的角色图片集成；核心聊天不要求它 |
| `IMAGE_STORAGE_DIRECTORY` | 可选生成图片目录；建议需要跨启动位置复用时指定固定绝对路径 |

不要用 `acceptance` profile 做普通 MySQL 使用：它是受保护的 H2 内存验收 profile。测试使用的 `soft-delete-test` 也不能用于日常数据。

## 3. 普通真实模式启动

在第一个 PowerShell 终端、项目根目录配置进程环境。下面的秘密用交互式输入，避免写入命令历史；示例地址与模型需换成自己的可用配置。

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local-real'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/companion_local?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8'
$env:DB_USERNAME = Read-Host 'MySQL username'
$databaseSecret = Read-Host 'MySQL password' -AsSecureString
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', $databaseSecret).Password
$jwtSecretInput = Read-Host 'JWT secret (at least 32 UTF-8 bytes)' -AsSecureString
$env:JWT_SECRET = [Net.NetworkCredential]::new('', $jwtSecretInput).Password
$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE = 'false'
$env:AI_PROVIDER = 'openai-compatible'
$env:AI_MOCK_ENABLED = 'false'
$env:AI_API_URL = Read-Host 'Full Chat Completions URL'
$env:AI_MODEL_NAME = Read-Host 'Enabled model name'
$providerSecret = Read-Host 'Provider API key' -AsSecureString
$env:AI_API_KEY = [Net.NetworkCredential]::new('', $providerSecret).Password
cd backend
mvn spring-boot:run
```

后端默认 `http://localhost:8080`。等待 Tomcat 和应用启动成功；不以 Maven 已开始编译作为启动通过。一次聊天还可能触发记忆、关系、成长调用，不能将一条消息理解为一次付费请求。

在第二个终端、项目根目录：

```powershell
cd frontend
npm ci
npm run dev
```

前端默认 `http://localhost:5173`，普通 Vite 配置把 `/api` 和 `/generated-images` 代理到 `http://localhost:8080`。若 8080/5173 被已有服务占用，先识别归属，不擅自停止；改变端口时必须同步检查代理。本轮普通默认端口实际启动通过。

## 4. Mock、缺配置与停止

在同一独立数据库、同一 `local-real` 配置下，设置 `AI_MOCK_ENABLED=true` 后重新启动，可使用内置 Mock 开发测试；它不证明真实 Provider 可用。测试结束改回 `false` 并重启。`AI_PROVIDER=mock` 同样选 Mock，真实模式应同时核对 provider 与开关。

真实模式缺少 Key 时会看到 `AI_API_KEY is required when mock mode is disabled` 并启动失败；模型、URL 缺失也有相应校验。网络、额度、模型权限问题需看实际错误，不能改用 Mock 冒充成功。

各自终端使用 **Ctrl+C** 停止；Windows Maven 若询问 `Terminate batch job (Y/N)?`，确认 `Y`。看到后端连接池关闭后再启动，不停止 MySQL 服务。重启时用同库和同配置；若更换 JWT secret，需要重新登录。数据库行不会因停止应用而删除。

本轮默认关闭 SQL 明细和无用的 Spring 默认账号生成；不要为排错开启包含密码哈希、token、完整私人对话的全量 SQL/Prompt 日志。

## 5. 文件与检查

- 从 `backend/` 执行 Maven 时，默认图片目录为 `backend/generated-images/`；它相对于**后端进程工作目录**，不是数据库内的图片二进制。不要切换启动位置后误判旧图片丢失。可用固定 `IMAGE_STORAGE_DIRECTORY` 复用同一目录。
- `/generated-images/` 是资源映射；图片文件与数据库都需保留。此阶段没有生成新图片，不代表所有图片 Provider 已验收。
- Redis 默认禁用，普通本地运行不要求启动 Redis。
- 注册／登录、USER 身份选择、角色聊天与刷新操作见[使用指南](USER_GUIDE.md)。验收范围见[当前验收摘要](ACCEPTANCE_STATUS.md)。

最终候选统一检查：

```powershell
cd backend
mvn test
cd ../frontend
npm test
npm run build
```

后端测试强制隔离 H2 内存库并关闭 Flyway，不能通过指向日常数据库来“修好”测试。
