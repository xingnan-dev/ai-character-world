# Docker Compose 本地运行

此方式将 Nginx 静态前端、Java 17 后端和独立 MySQL 一起运行在本机 Linux 容器中，不需要公网部署。宿主机无需另装 Java、Maven、Node 或 MySQL。普通非容器方式仍见 [LOCAL_RUN.md](LOCAL_RUN.md)。

## 前置条件与镜像

安装并启动 Docker Desktop，选择 **Linux containers**；Windows 需完成 Docker Desktop 要求的 WSL 2 配置。执行 `docker version` 应同时显示 Client 和 Linux Server，`docker compose version` 应可用。只有客户端输出、管道不存在或 daemon 不可访问表示引擎尚未就绪，不能开始运行验收。

构建需要访问镜像仓库、Maven Central 和 npm registry。仓库依赖版本不变，后端构建运行 `mvn package`（包含测试），前端运行 `npm ci`、`npm test`、`npm run build`。基础镜像锁定 digest；本次使用的镜像组合如下，不使用 `latest`：

| 用途 | 镜像标签（完整 digest 见 Dockerfile／compose.yaml） | 镜像内版本 |
| --- | --- | --- |
| 后端构建 | `maven:3.9.9-eclipse-temurin-17` | Maven3.9.9、Java17.0.15 |
| 后端运行 | `eclipse-temurin:17-jre-jammy` | Java17.0.19 |
| 前端构建 | `node:22-alpine` | Node22.23.5 |
| 前端运行 | `nginx:1.27-alpine` | Nginx1.27.5 |
| 数据库 | `mysql:8.0` | MySQL8.0.46 |

相同标签后面的不可变 digest 固定了实际内容，不会因标签移动而自动升级。构建出的本地应用镜像使用项目名前缀和 `:local` 标签。多架构机器若不能运行当前锁定镜像，应先核对平台并另行验证，不直接删除 digest 或改成 latest。

## 1. 准备本地环境文件

在仓库根目录执行一次，不覆盖已有配置：

```powershell
if (Test-Path .env.docker) { throw '.env.docker 已存在，请编辑原文件，不要覆盖' }
Copy-Item .env.docker.example .env.docker
```

用本地编辑器填写 `.env.docker`，不提交、不截图或粘贴到日志。普通 Spring `.env.example` 不能直接替代它。值包含 `$`、`#` 或空格时使用 Compose 支持的单引号值，避免插值；不要在值后随意追加注释。

| 变量 | 设置方法与作用 |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | 默认示例 `ai-companion-local`，决定网络、容器和命名卷前缀；后续必须保持相同 |
| `APP_PORT` | 默认8088；如已占用，换成空闲端口；只发布 `127.0.0.1:端口` |
| `DB_NAME`／`DB_USERNAME` | 首次启动创建的独立数据库／非root应用账号，默认 `companion_local`／`companion`；不要填写root作为应用用户 |
| `DB_PASSWORD` | 必填，自行生成强随机密码，无内置可用密码 |
| `MYSQL_ROOT_PASSWORD` | 必填，与应用密码分开管理；仅数据库容器使用 |
| `JWT_SECRET` | 必填，至少32个UTF-8字节；安全保存，重建保持相同可避免令牌失效 |
| `AI_PROVIDER` | 真实模式设 `openai-compatible` |
| `AI_MOCK_ENABLED` | 日常真实AI保持 `false`，不是仅填写Key即可 |
| `AI_API_URL` | 完整 Chat Completions endpoint，包含 `/chat/completions`；容器必须能访问 |
| `AI_MODEL_NAME`／`AI_API_KEY` | 提供自己有权限、有额度的真实模型名称与Key；不要保留示例模型/地址 |
| `AI_MAX_TOKENS` | 默认1024；聊天后还可能有记忆、关系、成长调用，并非总费用上限 |
| `ZHIPU_IMAGE_API_KEY` | 可选；不配置不影响核心聊天，不能因此宣称真实图片生成可用 |

Compose 的 `--env-file` 用于插值，**并不自动将所有变量注入容器**；`compose.yaml` 的 `environment` 明确映射需要的键。宿主机同名进程变量优先于该文件，排查冲突时检查变量是否存在，不输出秘密。不要执行未脱敏的 `docker compose config`、完整 `docker inspect` 或 `env` 并贴出其输出；静默验证用下一节命令。

后端固定活动 profile 为 `local-real`，构建排除本地dev／local配置，运行时不加载 `application-dev.yml`。数据库URL由Compose生成，主机为服务名 `mysql:3306`；无需提供宿主机 `DB_URL`。图片路径固定 `/app/generated-images` 并挂载独立卷。后端和MySQL均不发布宿主端口。

容器时区显式设为 `Asia/Shanghai`，与现有JDBC配置和本地使用习惯一致；避免默认UTC使现有不带时区的时间字段显示偏移。没有通过改写已有数据库时间来伪造一致。

显式Mock开发：将 `AI_MOCK_ENABLED=true` 后重新创建后端即可，无需真实Key；恢复真实模式改回 `false` 并重建容器。Mock结果不是Provider可用证明。真实模式缺Key、URL或模型时后端会明确启动失败；不自动降级到Mock。Flyway可能已在AI配置校验失败前执行，故仍须使用独立库。

## 2. 首次构建与启动

```powershell
docker compose --env-file .env.docker config --quiet
docker compose --env-file .env.docker up -d --build --wait --wait-timeout 300
docker compose --env-file .env.docker ps
```

默认浏览器地址：`http://localhost:8088`。如果改了 `APP_PORT`，使用对应端口。按 [USER_GUIDE.md](USER_GUIDE.md) 注册、选择USER身份、创建AI并聊天；从角色详情进入或刷新后可能需手动选择会话。

首次构建包含依赖下载、隔离H2测试、前端构建，会比日后启动慢。MySQL健康检查执行应用账号的实际SQL，确认就绪后才启动后端；Flyway按现有迁移初始化容器空库。后端HTTP鉴权入口就绪后Nginx启动，不以镜像构建成功代替应用启动。健康检查只证明基本就绪，不证明Provider额度或聊天质量。

## 3. 状态、日志与SSE

```powershell
docker compose --env-file .env.docker ps
docker compose --env-file .env.docker logs --tail 80 backend
docker compose --env-file .env.docker logs --tail 80 mysql
docker compose --env-file .env.docker logs --tail 80 frontend
```

分享日志前仍须检查脱敏；不启用包含私人对话或凭证的全量SQL/Prompt输出。镜像内不存在配置文件里的真实秘密，运行时环境属于本机Docker管理者可访问的数据。

Nginx同源代理 `/api/`、`/generated-images/`，Vue history路由使用index回退。API代理关闭缓冲、压缩和断开后继续转发，使用HTTP/1.1、300秒读写空闲超时，不自动重试上游请求。Docker DNS会重新解析backend服务名，支持后端换容器IP。

聊天可观察逐步输出；点击停止／刷新会断开SSE，服务端确认取消可能延迟，页面按既有恢复机制读回终态。未完成片段不持久化，不能保证Provider停止计算或计费。World／Agent的异步恢复语义与聊天不同，仍见[限制清单](KNOWN_LIMITATIONS.md)。

## 4. 停止、更新与重新创建

普通停止（保留容器和卷）：

```powershell
docker compose --env-file .env.docker stop
docker compose --env-file .env.docker up -d --wait --wait-timeout 300
```

更新源代码后构建并启动；本命令不自动执行Git合并：

```powershell
docker compose --env-file .env.docker up -d --build --wait --wait-timeout 300
```

要真正删除旧容器再创建、验证同卷恢复，先确保无重要任务正在执行：

```powershell
docker compose --env-file .env.docker down
docker compose --env-file .env.docker up -d --build --wait --wait-timeout 300
```

普通 `down` 删除本项目容器／网络，保留命名卷；重新创建不等于仅刷新页面或重启前端。所有命令必须使用同一环境文件和项目名。多人或多套运行可显式加 `-p 自己的项目名`，所有后续命令都要带相同 `-p`。不操作其他项目或本机MySQL。

## 5. 数据保留与常见问题

- MySQL数据在 `<项目名>_mysql_data`，图片在 `<项目名>_generated_images`，由Docker Desktop Linux存储管理，不绑定现有Windows数据库目录。图片卷仅后端可写，运行用户为UID10001；Nginx通过后端读取。
- **删除数据卷会清空相应数据。** 普通流程不使用 `down` 的 `--volumes`／`-v` 选项，不运行 `docker volume prune`。保留环境文件、相同项目名及卷；容器更新不能替代备份。
- 初始化后修改 `.env.docker` 的数据库密码不会自动修改已有MySQL账号密码。密码轮换需按MySQL方式同步修改账号和配置；不要通过清卷处理一次认证错误。
- 端口占用：修改 `APP_PORT` 后重新创建前端；不要停止原服务。数据库无需宿主映射，也不用占用本机3307。
- AI错误：确认 `AI_PROVIDER`、`AI_MOCK_ENABLED=false`、完整URL、Key、模型权限/额度；容器不能用 `localhost` 指向宿主Provider。宿主服务需要从容器可访问的地址，不能直接复用宿主回环URL。
- 数据库未就绪：查看mysql健康状态和日志、Docker磁盘空间；不要对日常库运行容器初始化。迁移只有现有Flyway执行，不修改已执行SQL。
- SSE不逐步显示：确认访问的是Nginx应用端口，未加其他会缓冲的代理；看frontend/backend日志。上游模型本身也可能首字慢，不能只凭等待时间归咎于Nginx。网络断开后以应用终态为准。
- 路由刷新404：确认使用仓库构建的Nginx镜像和history回退配置，而不是直接打开dist文件。

容器化只改变本地运行方式，不清洗旧记忆、不新增业务能力。历史库升级、真实Provider崩溃恢复及长期AI质量边界继续适用。

## 本次容器验收记录

2026-11-22，Docker Desktop4.86.0／Engine29.7.2、Compose5.3.1、Linux amd64引擎，使用独立项目与两个新命名卷：

- Linux镜像构建中实际执行后端 `mvn package`：510项，失败0、错误0、跳过1，打包成功；前端332项测试通过、生产构建通过。这是本次容器构建的新成绩，不借用第七阶段数字。
- MySQL健康后启动后端，空库V1–V27迁移通过，三个服务健康，只有Web端口发布到本机。
- 浏览器注册登录、创建USER／AI、详情history路由刷新、真实Qwen流式聊天和消息刷新读回通过。Nginx返回 `text/event-stream`、`X-Accel-Buffering: no`，观察到多次实际正文更新。
- 另一条真实请求在出现正文后刷新，应用恢复为 `CANCELLED`、输入可用；原会话随后继续真实聊天通过。没有宣称Provider停止计算或计费。
- 正常停止、移除三个旧容器，再以同卷创建三个新容器；容器ID全部改变，浏览器重新登录后角色、会话、消息及取消状态一致。图片目录以非root用户写入明确标记的测试文件，重建前后同源HTTP读回与SHA256一致，**未调用真实图片生成API**。
- 最终补齐显式 `Asia/Shanghai` 后再次重建，JVM诊断与应用日志确认时区，迁移仍up-to-date；浏览器读回和测试文件再次通过。没有修改先前专用测试记录的时间或内容。
- 真实模型请求记录7次（6完成、1主动取消），可获得 `total_tokens` 合计7586；取消请求用量未返回，未核对账单。重建及读回未新增模型调用。

详细脱敏报告和截图位于原验收工作区的 `.acceptance-run/docker-local/`，不随代码提交，也不是运行依赖。这里只证明所列本机Linux容器案例，不扩写为历史数据库升级、真实Provider崩溃恢复或所有硬件平台通过。
