---
name: phase-review
description: Use when asked to review or check the real completion status of a Phase or sub-phase against its goals. Read-only — reads actual code, tests, migrations, and config; runs tests only to establish current results; never modifies code; reports PASS / NEEDS FIX / BLOCKED with blocker / risk / improvement separation.
---

# phase-review

只读审查指定 Phase / 子阶段的真实完成情况，不修改任何代码。

## 步骤
1. 确认审查范围：目标 Phase / 子阶段及其预期目标（从提交历史、相关代码与上下文确认）。
2. 读取实际代码与测试，逐项对照目标：
   - 前端：`frontend/src/`（views / stores / api / utils）与 `frontend/tests/`
   - 后端：`backend/src/main/java/com/companion/`（controller / service / mapper / entity / dto）
   - 数据库：`backend/src/main/resources/db/migration/`（V1–V19）
   - 测试：`backend/src/test/` 与 `frontend/tests/`
3. 需要「当前」测试结果时，实际执行命令并记录输出；不引用历史验收报告中的数字。
4. 只读，不修改任何代码、配置或数据库。

## 输出
对每个检查项给出结论，并汇总为 **PASS / NEEDS FIX / BLOCKED**，分三类列出问题：
- **blocker**：功能未实现或不可用，阻塞本阶段验收
- **risk**：存在隐患或潜在不一致，需关注但非立即阻塞
- **improvement**：可改进项，不影响验收

同时列出「当前测试结果」与「未验证项」，无法确认的信息明确标注「需要确认」。
