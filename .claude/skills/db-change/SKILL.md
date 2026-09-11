---
name: db-change
description: Use when handling any database schema or Flyway migration task — adding tables/columns, changing fields, or fixing migration drift. Checks existing migrations and related entity/mapper/DTO first, never modifies already-executed migrations, always adds a new migration for schema changes, asks before touching soft-delete-schema.sql or other historical/test schemas, verifies test vs production schema consistency, and never performs destructive database operations.
---

# db-change

处理数据库 Schema / Flyway 迁移任务，遵守 CLAUDE.md 的 Database / Flyway Rules。

## 步骤
1. 先检查现有 migration（`backend/src/main/resources/db/migration/`，V1–V19）与相关 Entity / Mapper / DTO，确认目标表结构与字段现状。
2. 已执行的 migration 永远不修改；Schema 变化一律新增 `V{n}__*.sql`。
3. 对 `soft-delete-schema.sql`（main 与 test 各一份）等历史 / 测试 Schema 不确定时，先询问用户，不自行统一或删除。
4. 新增 migration 后，检查测试 Schema 与生产 Schema 一致性（`backend/src/test/resources/soft-delete-schema.sql`、`application-soft-delete-test.yml`），必要时同步并说明。
5. 不执行破坏性操作（drop / truncate / 直接改已执行 migration / 清库）。

## 完成后报告
新增 migration 文件、字段 / 索引变更、测试 Schema 是否同步、执行结果与风险。
