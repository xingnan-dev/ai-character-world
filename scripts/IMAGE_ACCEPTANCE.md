# 图片人工验收环境

在仓库根目录打开两个 PowerShell 终端：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-image-acceptance.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start-image-acceptance-frontend.ps1
```

访问 `http://127.0.0.1:15173`，在注册页创建 `acceptance@example.test` / `Accept123!`。后端只使用 H2 内存库和 `.acceptance-run/images`，关闭 Flyway；停止后端会清空 H2，普通页面刷新不会。验收 profile 的每日额度为 100，失败记录仍计数，并发限制仍为每用户 1。

验收步骤：创建 AI Character，输入外貌并生成、预览、保存；刷新详情核对图片。生成描述填 `FAIL` 会得到可恢复失败，改成普通描述即可重试。要让确认只失败一次，在浏览器开发者工具 Network overrides 或请求工具中给确认请求加唯一请求头 `X-Acceptance-Fail-Confirm-Once: 任意唯一值`，同值重试会进入真实确认流程。要临时验证回退头像，在开发者工具 Network request blocking 中阻止当前 `/generated-images/...` 请求后刷新，不修改数据库或图片 URL。

图片资源遵循浏览器 `<img>` 可直接加载的静态资源设计：匿名和登录状态访问均返回 200；生成与确认 API 仍要求 JWT。

两个终端分别按 `Ctrl+C` 停止。仅需清理本次图片文件时执行：

```powershell
Remove-Item -LiteralPath .\.acceptance-run -Recurse -Force
```

## 本轮验收记录（phase-c6）

- 自动化：图片服务回归 8/8；前端完整测试 175/175；生产构建成功。
- HTTP：注册、登录、创建角色、fake 生成、PNG 读取、确认绑定、重新查询及前端图片代理均已实际验证；确认后返回 `visualType=IMAGE`，角色 `imageUrl` 与候选地址一致。
- 人工页面：当前已保存图片与待确认候选分开展示；保存后详情显示新候选图；换图后角色资料保持完整。
- 未覆盖：真实智谱、真实数据库历史影响、历史 Chat/World/Round 快照页面兼容、多实例额度与并发原子性。生成图片当前可凭链接匿名访问。

## 回归收尾记录

常驻验收入口已改为 `AcceptanceServerLauncher`，不再匹配默认 Surefire 的 `*Test` 扫描；启动脚本通过 `-Dtest=AcceptanceServerLauncher` 显式运行它。此前未结束的原因是旧入口中的 `CountDownLatch(1).await()` 被普通完整回归扫描，属于线程常驻，不是业务失败或外部请求等待。

最终后端命令使用 JDK 17、repo-cache、隔离 H2、`spring.flyway.enabled=false` 执行，耗时 45.667 秒：274 项，273 通过，0 失败，1 跳过（真实智谱手工测试）。前端本轮不重复执行：此前完整测试 175/175 通过，生产构建成功。
