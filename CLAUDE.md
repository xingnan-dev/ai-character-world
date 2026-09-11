# CLAUDE.md

AI Virtual Companion —— AI 3D 数字分身聊天平台（Vue 3 前端 + Spring Boot 3 后端）。
本文件为项目唯一的长期开发约定来源（项目无 README）。

## Project Overview
AI 虚拟伴侣应用：VRM 3D 形象、AI 角色创建与图片生成、SSE 流式聊天、多角色世界
（world）回合编排、记忆检索。按 phase 阶段迭代开发（当前 phase-c7）。

## Tech Stack
- 后端：Java 17、Spring Boot 3.2.5、Spring Security(JWT)、MyBatis-Plus 3.5.5、
  Flyway、MySQL 8、Redis(默认禁用)、JJWT、Lombok、Hutool、OkHttp、Reactor Core
- AI：OpenAI-compatible 协议 + Mock provider；图片生成走智谱（cogview-3-flash）
- 前端：Vue 3.4、Vite 5.2、Vue Router 4、Pinia(+persistedstate)、Axios、
  Element Plus、Three.js + @pixiv/three-vrm、Sass
- 测试：后端 JUnit5 + H2(MySQL 模式)；前端 Node 内置 test runner

## Project Structure
- `backend/`：Spring Boot，分层 controller / service(接口)+service/impl / mapper /
  entity / dto；领域包 ai、avatar/generation、character、chat、world、image、
  security、config
- `backend/src/main/resources/db/migration/`：Flyway 迁移（V1–V19）
- `backend/src/main/resources/prompts/`：LLM prompt 模板（*.md）
- `frontend/src/`：views / components / stores / api / utils / three / avatar / router
- `frontend/tests/`：Node 内置测试（*.test.js）
- `scripts/`：图片人工验收脚本与说明

## Architecture
- 后端经典分层；MyBatis-Plus 用 `BaseMapper` + 注解，不新增 XML mapper
- 鉴权：无状态 JWT + 会话黑名单（t_auth_session）；放行 `/api/auth/**`、
  `/api/avatar/list/public`、`/generated-images/**`
- 前端状态放 Pinia store；可单测的纯逻辑放 `frontend/src/utils/*.js`
- AI 接入遵循 `LlmProvider` 抽象，prompt 用 classpath 模板，不硬编码到业务代码

## Development Rules（工作流程）
- 修改前先读取相关实现与调用方，不基于未读代码猜测
- 优先最小范围修改，避免无关重构
- 前后端接口、DTO、数据库字段发生变更时，检查对应调用链
  （controller/service/mapper/前端 api/store/测试）
- 修改后运行相关测试；不为了让测试通过而降低断言或删除测试
- 遵循 Conventional Commits（feat/fix/test/style/…），按当前 phase 分支开发
- 所有结论来自实际代码与 Git，不确定的信息明确标注「需要确认」

## Backend Rules
- Java 17；沿用既有分层与命名（表前缀 `t_`、根包 `com.companion`）
- 配置走 `application.yml` 的环境变量占位（如 `${DB_URL:…}`）；本地敏感值放
  gitignored 的 `application-dev.yml`，不提交真实密钥
- dev 使用 mock AI provider；真实 provider 依赖环境变量注入

## Frontend Rules
- Vue 3 Composition API；路由鉴权走 `router.beforeEach` + `meta.requiresAuth`
- 组件按域放 `components/<home|chat|world|layout|ui>/`；跨页复用逻辑放 `utils/`
- 新逻辑尽量写成可单测的纯函数，并补 `frontend/tests/*.test.js`

## Database / Flyway Rules
- Flyway 是唯一权威 schema 来源；新表/新列只能通过新增 `V{n}__*.sql` 迁移
- 已执行的 migration 不允许修改（会触发校验失败）
- 沿用既有约定：`t_` 前缀、`deleted` 逻辑删除列、`status` 表示业务/上下架状态、
  `create_time`/`update_time` 时间戳、utf8mb4
- 对 `soft-delete-schema.sql` 等历史/测试 Schema 不确定时先与用户确认，不自行
  统一或删除

## Testing & Verification
- 后端：`mvn test`（测试强制 H2 内存库 + `flyway.enabled=false`，违反即启动失败）
- 前端：`npm test`（`node --test tests/*.test.js`）
- 构建：后端 `mvn package`、前端 `npm run build`
- 图片人工验收：按 `scripts/IMAGE_ACCEPTANCE.md` 启动验收环境
- 历史验收报告中的测试数字不作为当前验证结果；当前结果必须实际执行后报告

## Git Safety Rules
- 未经明确授权，不执行 git add、commit、push、pull、merge、rebase、reset、
  checkout、clean
- 不删除或覆盖用户的现有修改；不 force-push 到共享分支
- 提交范围保持聚焦，不混入生成物或本地配置（dist/、target/、generated-images/、
  application-dev.yml、.acceptance-run/）

## 现状备注（仅记录，不自行变更）
- Redis：依赖与工具类存在但默认禁用（`app.redis.enabled=false`），不自行启用或删除
- Maven Wrapper：当前缺失，构建用系统 Maven + JDK 17，不自行添加

## Final Report Format
每次完成工作后报告：
- **Changed Files**：改动文件及原因
- **Tests Run**：实际执行的测试命令
- **Test Results**：实测结果（含关键输出）
- **Remaining Risks**：遗留风险或待确认事项
- **Git Status**：当前分支与工作区状态
