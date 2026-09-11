---
name: regression
description: Use when asked to run regression verification on a completed feature or change. Selects backend, frontend, and build tests based on the change scope, reports the exact commands and results actually executed, analyzes real causes of failures, and does not modify code unless the user explicitly asks to enter a fix phase.
---

# regression

对已完成的功能进行回归验证，默认不修改代码。

## 步骤
1. 根据改动范围选择测试面：
   - 后端改动 → `mvn test`（测试强制 H2 内存库 + `flyway.enabled=false`）
   - 前端改动 → `npm test`（`node --test tests/*.test.js`）
   - 构建验证 → 后端 `mvn package`、前端 `npm run build`
2. 实际执行命令，报告真实命令与结果（含关键输出），不引用历史测试数字。
3. 失败时分析真实原因（读代码、看报错输出），区分业务缺陷 / 测试配置 / 环境问题。
4. 不修改代码，除非用户明确要求进入修复阶段。

## 输出
汇总为 **PASS / FAIL / BLOCKED**：
- **PASS**：所选测试面全部通过
- **FAIL**：有测试失败，附原因分析
- **BLOCKED**：因环境 / 依赖 / 外部服务无法执行，附阻塞原因
