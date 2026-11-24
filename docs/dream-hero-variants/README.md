# 浮光卷 · 首页主视觉 A/B 局部方案

本目录记录在已确认第二版「浮光卷 / Dreamlight」之上制作的两个**首页主视觉局部方案**，
用于比较，不代表最终选定方向。A、B 只替换首页 hero 的视觉拼贴与少量预览标识；导航、
文案、hero 高度、主按钮、角色入口、世界入口和聊天页布局均沿用 v2。

- 分支：`style/dreamlight-collage-preview`
- v2 基线提交：`24f3407a35b41df9988e09ff9261da164c2e7633`
- 预览服务：<http://127.0.0.1:5199/>（独立 Vite 预览，端口固定 5199）
- 范围：仅 `frontend/preview/` 与本文档；不修改正式业务页面、正式默认主题、数据库或
  配置；不更新 `localhost:8088` 的正式容器；不做实时 3D、真实 AI、业务接口或数据库读写。

## 预览地址与切换

| 方案 | 独立地址 | 回看方式 |
| --- | --- | --- |
| A · 物件拼贴 | <http://127.0.0.1:5199/#/home?hero=a> | 直接访问 URL |
| B · 人物剪影（占位） | <http://127.0.0.1:5199/#/home?hero=b> | 直接访问 URL |
| v2 · 浮光卷基线 | <http://127.0.0.1:5199/#/home> | 无 `hero` 参数即 v2 |

- 首页 hero 右上角有一个独立「主视觉」预览控件，可在 A / B / v2 之间切换；它只属于
  设计预览，未混入顶部产品导航。选择后控件自动收起，URL 同步为 `?hero=a`、`?hero=b`
  或回到无参数 v2。
- 聊天页：<http://127.0.0.1:5199/#/chat?session=rain>；会话参数仍为 `tomato`、`rain`、
  `record`、`rooftop`、`snack`。聊天页不读取 `hero` 参数。
- 启动：`cd frontend && npm run dev:design`。停止：运行终端 `Ctrl+C`。

## 两个方案如何表达「自由拼贴 / 复古小装饰」

### A · 物件拼贴 / OBJECT COLLAGE

- 主体是一组悬浮小物件：玫瑰与冰色水晶挂件、打开的缎带蝴蝶结、金色吊链、珍珠、
  像素爱心、轨道细线和星芒；不再以单个金属环 + 材质球作为唯一中心。
- 物件通过前、中、后三层叠压：吊链从右上穿过，水晶压在蝴蝶结与轨道之上，像素爱心
  落在蝴蝶结左下方；局部柔焦、投影和边缘遮罩让图像与蓝粉光、页面底色自然相接，而
  不是一张带硬边框的产品图。
- 标题、正文、继续入口在左侧；hero 构图从 31% 处起，轨道和吊链越过文字区边界，形成
  图像—文字—装饰相互穿插的自由拼贴，而不是「左文右卡」。
- 复古趣味来自小尺寸的像素爱心、星芒、金色细链和右侧 `MATERIAL STUDY · N°02` 注释；
  装饰克制，未铺满页面。

### B · 人物剪影 / SILHOUETTE STUDY

- 主体是一个原创虚构人物的侧脸剪影：手写的样条轮廓、发束、肩线和细项链；与暖珍珠
  光环、柔焦叠影、局部颗粒、轨道和像素爱心吊坠结合，不采用硬边框角色卡。
- 人物只表达一般性的「有人在」的梦境氛围，不绑定具体角色、关系或世界观；以短发
  轮廓减少性别指向，保留通用的人物气质。文案与 A 完全相同，避免 B 变成另一个
  产品故事。
- **B 是程序化剪影占位，尚未达到成品人像质量。** 本地没有可用的原创人物图像生成能力，
  也没有用外部人物照片或参考图像像素；B 的元素由离线 Pillow/NumPy 轮廓、渐变与遮罩
  合成。页面左上角标识与预览控件内均明确写有「剪影占位 · 未完成」，**不应把 B 当作
  已完成的人像方案或可直接上线的角色资产。**
- 如未来要完成 B，需要进入有授权的原创人物图像生成/绘制环节；当前分支没有假装达到
  该目标。

## 保持不变的部分

- 顶部导航、品牌、文案、两行中文标题、按钮文字、继续会话、四个角色入口和世界入口
  与 v2 一致；A/B 未增删业务元素。
- hero 高度、角色区起点和首屏结构在 1440×900、1024×768、390×844 下与 v2 相同：

  | 视口 | hero 高度 | 角色区 y | 首张角色卡 y / 底 | 结论 |
  | --- | --- | --- | --- | --- |
  | 1440×900（v2 / A / B 相同） | 612 | 677 | 799 / 921 | 角色名、身份、摘要与按钮可识别、可点击；卡片底部 21px 在首屏下缘之外 |
  | 1024×768（A / B） | 608 | 673 | 787 / 909 | 首屏出现「四种脾气，四张门」标题；中间宽度不要求完整卡片 |
  | 390×844（v2 / A / B 相同） | 639 | 696 | 825 / 921 | 标题与首张卡顶部 19px 入屏，与 v2 相同 |

- 页面无横向溢出（三档宽度均 `scrollWidth == innerWidth`）。未缩小整页、未压缩导航、
  未改变 hero 高度来换取首屏卡片。
- A/B 只把全页颗粒降为 `0.055`，纹理更多集中在 hero 局部；v2 保持 `0.1`。正文、按钮、
  卡片仍位于干净底色上。
- 中文标题断行不变；无新增遮挡按钮、角色名或正文的艺术元素。手机宽度只对 A/B 的
  cut-out 做 `scale` 取景，hero 容器高度与文字位置不变。

## 聊天页的轻量呼应

- 沿用 v2 的两栏/抽屉、阅读列宽度、紧凑输入区、折叠资料与窄抽屉，没有第二套聊天页。
- 只在氛围层加入一枚 15px 像素爱心（与首页同源的 `PixelHeart.vue`）；它位于消息画布
  之外的右侧，不占消息空间，≤960px 隐藏。桌面可见、390px 不可见。
- 消息气泡、输入区、会话列表、资料层没有改用拼贴装饰；未增加闪烁、鼠标拖尾或重动画。
- 现有角色头像仍是 v2 的抽象材质裁切，**不是人像身份插画**；A/B 没有把抽象物件头像
  宣传成人物身份方案。

## 原创素材、来源与使用限制

**没有使用任何外部图片、参考图像素、图像生成模型、扩散模型、Blender、Three.js 渲染
或实时 3D。** 两张 hero 资产都是本地离线程序化静态位图：

| 文件 | 尺寸 / 体积 | 制作方式 | 来源 |
| --- | --- | --- | --- |
| `frontend/preview/assets/art/hero-collage-object.webp` | 2400×1600 透明，172,570 B | 手写缎带、水晶、吊链、像素爱心与星芒的矢量/解析渐变合成，Pillow + NumPy 光栅与柔焦 | 本分支新增；无外部像素 |
| `frontend/preview/assets/art/hero-collage-figure.webp` | 2400×1600 透明，123,544 B | 手写样条人物轮廓、短发型剪影、肩线、项链与像素爱心吊坠；局部材质只取自本项目自己的 `world-dream.webp` 模糊亮度/色彩 | 本分支新增；非人像生成 |
| `frontend/preview/assets/art/world-dream.webp` | 既有 v2 资产 | v2 已提交的原创程序化丝光材质样张 | B 仅作为模糊材质纹理复用，不引入新授权 |
| `frontend/preview/assets/art/grain-tile.png` | 既有 v2 资产 | v2 原创颗粒平铺 | A/B hero 局部纹理 |

- A/B 的 PNG 母版与生成脚本仅在本机 `.acceptance-run/dream-hero/` 保留，不提交；仓库只
  提交页面实际引用的 WebP 产物。
- 使用限制：这些图是**程序化视觉样张**，不是摄影、不是真人肖像，也不是可直接商用的
  成品角色插画；仅用于本设计分支的视觉/布局评审。B 尤其不得被描述为成品人像。字体
  （Noto Serif SC / Noto Sans SC / Bodoni Moda / Pinyon Script）为本地自托管 OFL 1.1，
  许可证位于 `frontend/preview/assets/fonts/licenses/`。
- 三张风格参考仅用于分析，未裁切、拼贴、嵌入、作为纹理或复制其人物、文字、水印。

## 实际检查结果

- 浏览器：系统 Chrome + Playwright Core；无 console error、无 pageerror（`errors: []`）。
- 1440×900：v2 / A / B 的 nav、hero 高度、角色区起点、首张卡位置一致；A/B 可经右上角
  预览控件切换，控件面板展示 A / B / v2 与 B 的占位警示；`?hero=a`、`?hero=b` 独立
  URL 可直达；选择 v2 后 URL 回到 `#/home`。390px 下展开控件面板为 x=92…378，
  完整落在视口内，B 占位警示全文可读，无横向溢出。
- 1024×768：A/B 无横向溢出，首屏出现角色区标题；未见装饰遮挡正文或按钮。
- 390×844：A/B 放大取景后主体仍可辨认；无横向溢出；主标题、继续按钮、角色入口可用；
  A 的小号材质注释在窄屏加浅色背衬以保持可读。
- `继续上次` 按钮在 B 下点击后进入 `#/chat?session=tomato`，聊天房间存在；未调用正式
  业务 API。
- 角色卡与导航抽测：v2 / A / B 下点击首张角色卡均进入 `#/chat?session=tomato` 且聊天
  房间存在；A / B 下点击顶部「角色」仍滚动到角色区（`charactersTop: 82`），首张卡
  中心点命中的是卡片本身；390px B 下角色卡同样可点击。
- reduced motion：全局动画/过渡被压至 0.01ms，页面仍可读；无闪烁或鼠标拖尾。
- 聊天：1440 可见像素爱心回声，390 隐藏；消息区宽度与输入区未变化。
- 前端测试：`npm test` → 342 tests, 342 pass, 0 fail。
- 设计预览构建：`npm run build:design` → 成功，产物 `frontend/dist-design/`（gitignored）。
- 正式前端构建：`npm run build` → 成功，`frontend/dist/`（gitignored）；仍出现既有的
  chunk > 500 kB 提示，与本次预览改动无关。
- 预览服务：5199 返回 HTTP 200，PID 29372，无重复监听；正式 8088 未修改、未重启
  （本次检查时 8088 未监听、HTTP 000，未启动正式容器）。

### 截图

均为实际浏览器截图；docs 内为保持原始尺寸的压缩 WebP（1440×900 / 1024×768 / 390×844），
原始 PNG 保存在本机 `.acceptance-run/dream-hero/shots/`（不提交）。

| 文件 | 内容 |
| --- | --- |
| [`screenshots/home-v2-1440.webp`](screenshots/home-v2-1440.webp) | v2 基线，1440×900 |
| [`screenshots/home-a-1440.webp`](screenshots/home-a-1440.webp) | A，1440×900，首屏角色入口可见 |
| [`screenshots/home-b-1440.webp`](screenshots/home-b-1440.webp) | B，1440×900，首屏角色入口可见 |
| [`screenshots/home-b-1440-dock-open.webp`](screenshots/home-b-1440-dock-open.webp) | B 预览控件展开，含 B 占位警示 |
| [`screenshots/home-a-1024.webp`](screenshots/home-a-1024.webp) | A，1024×768 |
| [`screenshots/home-b-1024.webp`](screenshots/home-b-1024.webp) | B，1024×768 |
| [`screenshots/home-v2-390.webp`](screenshots/home-v2-390.webp) | v2，390×844 |
| [`screenshots/home-a-390.webp`](screenshots/home-a-390.webp) | A，390×844 |
| [`screenshots/home-b-390.webp`](screenshots/home-b-390.webp) | B，390×844 |
| [`screenshots/chat-1440.webp`](screenshots/chat-1440.webp) | 聊天页，像素爱心回声 |
| [`screenshots/chat-390.webp`](screenshots/chat-390.webp) | 聊天页窄屏，回声隐藏 |

## 本次改动文件

- 新增 `frontend/preview/components/HeroPreviewDock.vue`：首页局部预览控件与 B 警示。
- 新增 `frontend/preview/components/PixelHeart.vue`：A/B 与聊天共用的 8×7 像素爱心。
- 新增 `frontend/preview/assets/art/hero-collage-object.webp`、
  `hero-collage-figure.webp`。
- 修改 `frontend/preview/views/HomeView.vue`：`hero` 查询参数、A/B 构图与手机取景、
  预览标识；v2 默认渲染保持不变。
- 修改 `frontend/preview/App.vue`、`frontend/preview/preview.scss`：仅在 A/B 时降低全页
  颗粒，hero 保留局部纹理。
- 修改 `frontend/preview/views/ChatView.vue`：加一枚不占阅读空间的像素爱心回声。
- 新增本目录。没有修改 `frontend/src/`、后端、数据库、主题配置或正式容器。

## Git 状态

- 当前分支：`style/dreamlight-collage-preview`，基于已确认 v2 提交
  `24f3407a35b41df9988e09ff9261da164c2e7633`。
- 本阶段改动只包含 `frontend/preview/` 与 `docs/dream-hero-variants/`；构建产物
  `frontend/dist/`、`frontend/dist-design/` 与本地取证目录 `.acceptance-run/` 均被
  gitignore，不进入提交。
- 不修改正式业务页面、默认主题、后端、数据库或配置；不更新 8088 正式容器；不把分支
  合入 `develop` / `main`，不创建合并 PR。

## 未完成 / 待确认

1. **B 的人物仍是程序化剪影占位**，没有达到成品人像质量；若 B 方向被选中，需要另行
   取得有授权的原创人像资产后再继续。
2. 1440×900 首张角色卡底部约 21px 在首屏下缘之外；卡片名称、身份、摘要和按钮已可见、
   可操作，与 v2 相同。没有通过缩页或压导航改变这一结构。
3. 390×844 下首张卡仅顶部 19px 入屏；与 v2 相同，属于既定 hero 高度与信息结构。
4. A 的小号 `MATERIAL STUDY` 注释在窄屏位于画面内，已加浅色背衬；若认为注释本身应
   完全移出图像，可再做位置调整。
5. B 右上角预览标签在手机宽度会压住一部分发束/装饰；它属于预览控件，不遮挡姓名或
   按钮。展开面板后已按右边缘对齐，完整位于视口内；若需要，可再改成更小的手机形态。
6. 未做正式页面接入、真实 API/DB/AI、真实人物头像替换、生产素材授权与商用评估。

**当前状态：等待用户比较 A / B / v2 并选择方向；未声明最终设计，未合入 develop/main，
未创建合并 PR。**
