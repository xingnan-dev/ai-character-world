---
name: implement
description: Use when implementing a feature or change according to an already-approved plan. Reads relevant code before editing, makes minimal changes, keeps frontend/backend contracts consistent, follows Flyway rules for schema changes, runs relevant tests afterward, and never runs git add/commit/push.
---

# implement

根据已确认的方案实施功能，遵守 CLAUDE.md 的开发规则。

## 步骤
1. 先读取相关实现与调用方（controller / service / mapper / entity / dto / 前端 api / store / 测试），不基于未读代码猜测。
2. 最小范围修改，避免无关重构。
3. 保持前后端接口一致：DTO / 接口字段 / 数据库字段变更时，检查对应调用链。
4. 数据库 Schema 变更遵守 Flyway 规则：新增 `V{n}__*.sql`，不修改已执行的 migration。
5. 修改后运行相关测试；不为了通过测试而降低断言或删除测试。
6. 不执行 git add、commit、push。

## 完成后报告
按 CLAUDE.md 的 Final Report Format 报告：Changed Files / Tests Run / Test Results / Remaining Risks / Git Status。
