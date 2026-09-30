# BiliPai 性能优化计划 2026Q4

> 编写日期：2026-10-01。
> 输入：`docs/perf/baseline-2026Q4.md`（实测基线）、`docs/perf/hotspots-2026Q4.md`（热点清单）、`docs/PERFORMANCE_REVIEW_PLAN.md`（上游审查计划）。
> 关键代码点已抽查核实：弹幕三重排序（`ByteDanceDanmakuEngine.kt:103/:296-298`、`DataManager.kt:50-54`）、转场 live 路径逐帧 `new BlurEffect`（`VideoCardTransitionBackgroundPolicy.kt:768-777`，同文件 overlay 版本 `:312-352` 已有缓存）、骨架屏开关反转（`HomeFeedSkeletonCard.kt:51-66`）。
> 纪律：每个修复走「归因 → 修复 → 同命令复测 → 数字回写基线」闭环；遵守 AGENTS.md 验证阶梯与打包政策；每完成一个有意义的切片 commit + push。

---

## 0. 摘要

当前性能问题的全貌可以压缩成一句话：**每个帧预算内，系统在重复计算本可缓存的东西、执行本可不执行的工作、并以无预算的方式消耗 GPU**。

- 最严重的问题是卡片转场在中端机 120Hz 下实际只有 ≈7–10fps（H-1，每帧耗时是预算的 ~18×），其次是弹幕全量排序阻塞主线程（H-2/H-3，静态估算跨窗 seek 阻塞 50–120ms）、播放器每次进详情全量重建（H-4）、直播同屏 10–22 个实时 blur 实例（H-6）、以及两个使一切相关优化失效的行为 bug（H-7 骨架屏开关反转、H-8 入场动画门控恒 false）。
- 计划分 8 个阶段（Phase 0 + A–G），总计约 **10–12 个工作日**，按「可感知度 × 收益确定性 ÷ 成本」排序，并以「测量先行」为硬前置。
- 量化目标：转场 OPENING p50 从 147ms 降到 <33ms（必须）/ <16.7ms（力争）；弹幕 `BiliPaiDanmakuSetData` 主线程 <8ms；滚动 jank 率维持 <5%（已有门禁防回归）；直播同屏实时 blur 实例 ≤3。
- 所有结论与复测命令以 `baseline-2026Q4.md` §4 固化命令为准，本计划不另造测量口径。

---

## 1. 第一性原理：从物理不变量到优化决策

性能优化不是「调参数」的集合，而是让系统行为重新服从几条不可协商的物理事实。本节先列出事实，再推导原则，最后用算术直接对本项目的方案排序与排除。后续所有阶段的取舍都引用本节，避免「拍脑袋优先级」。

### 1.1 六条物理事实

**F1. 帧预算是硬契约。**
每帧可用时间 = 1000ms ÷ 刷新率。中端机 120Hz（实测 vsync 121.4Hz）→ **8.24ms**；60Hz → 16.7ms。超预算 = 必丢帧，没有「稍微超一点」的灰色地带。任何优化最终只有两条路：把每帧工作量降到预算内，或者合法地改变预算本身（见 F6）。

**F2. 像素处理成本 = 像素数量 × 每像素操作数 × 读写带宽。**
全屏 1080×2400 ≈ 260 万像素；一次全屏离屏合成（saveLayer / RenderEffect blur）意味着每帧多分配 ~10MB（ARGB8888）并多一次全屏读写。降采样系数 s 使像素数变为 s²：0.82 降采样 ≈ 成本 ×0.67，0.5 降采样 ≈ 成本 ×0.25。**模糊类优化的杠杆几乎全在 s² 上**，不在微调半径上。

**F3. 主线程时间是串行且排他的。**
一帧内主线程做的每一件事都在排队。排序 6 万条数据（O(n log n) + 装箱 + 对象分配）约 10–50ms，等于一次性吃掉 1–6 个帧预算；同样数据的二分定位插入是 O(log n) 定位 + memmove 级搬移，<0.1ms。**复杂度等级的差距无法用常数优化弥补。**

**F4. 相同输入的重复计算是纯浪费。**
同一份数据排三次序（H-2）、每帧 new 一个参数相同的 BlurEffect（H-18）、每次进详情重建结构相同的 ExoPlayer（H-4）——本质都是「计算结果未被缓存」。判据很简单：**如果输入没变，第二次计算就不该发生**。这一类浪费的修复收益是确定的（不需要归因就知道有收益），只是大小待测。

**F5. 不执行的代码是最快的代码。**
mask 开启但无活动 mask 帧时仍每帧全屏 saveLayer（H-11）；用户关闭骨架屏「呼吸」后动画仍在跑（H-7）；门控条件恒 false 使「快滑时关动画」从不生效（H-8）；诊断关闭时仍拼字符串（H-17）。这些是**零收益支出**——功能上不产生任何价值，却持续消耗预算。它们应排在所有「调优」之前，因为它们既是浪费又是行为 bug。

**F6. 用户感知的是变化，不是绝对数字。**
人对 <300ms 窗口内的帧率减半（120→60Hz 呈现）几乎无感，但对 7fps 的 morph（卡顿、跳变）极敏感。反过来，稳态滚动的 p50 从 13ms 优化到 11ms 用户毫无感知。**优化预算要花在「变化被感知」的地方**：转场起止、进出场瞬间、交互响应。同时这也给出一个合法手段：短瞬态内可以牺牲保真度/帧率换预算（转场 60Hz 化、滚动期降采样），长稳态不可以。

### 1.2 由事实推导的设计原则

| 原则 | 来源 | 在本项目的映射 |
|---|---|---|
| P1 测量先行：不可测量则不可优化 | F1 | 凭据阻塞（§2.3）必须最先解除；每个修复前后跑同一固化命令 |
| P2 消灭重复计算，而非加速它 | F4 | 排序一次全链路信任（H-2/H-3）、BlurEffect 缓存对齐（H-18）、PlayerFactory 收敛（H-5） |
| P3 消灭不该执行的执行 | F5 | H-7/H-8/H-11/H-17 全部排在各自维度的最前面 |
| P4 降复杂度等级优先于降常数 | F3 | 弹幕通路 O(n log n)×3 → 窗口化 O(w)；插入 O(n log n) → O(log n) |
| P5 像素量是 GPU 优化的主杠杆 | F2 | 转场景深 blur 降采样（B 阶段）、直播气泡去 blur（E 阶段） |
| P6 状态作用域 = 重组扇出半径 | F3（Compose 放大器） | 高频状态下放到消费点（H-9） |
| P7 瞬态可牺牲保真，稳态不可 | F6 | 转场期 60Hz/降采样合法；常驻界面不可 |
| P8 优化会移动瓶颈，循环归因 | F1 | 每阶段以「归因 → 修复 → 复测 → 再归因」推进，不批量盲改 |
| P9 无门禁的性能必然退化 | F1 | 性能是熵：G 阶段把门禁固化进 CI，阈值取修复后基线 |

### 1.3 用算术直接裁决方案（本计划的排序依据）

**(a) 转场：60Hz 化单独不可行，必须先减工作量。**
OPENING p50 = 147.02ms，120Hz 预算 8.24ms → 超载 **17.8×**；RETURNING 99.31ms → 12.0×。转场期 60Hz 化只把预算翻到 16.7ms，仍超 8.8×。结论由算术直接得出：**「强制 60Hz」只能作为放大器，不能作为主修**；主修必须把每帧工作量降到 1/8 以下（全屏 blur 降采样 s=0.5 → GPU 成本 ×0.25 是唯一有此数量级的手段，见 F2）。修复方向排序因此确定为：①归因确认 blur 占比 → ②降采样/减层 → ③60Hz 化（可选放大器）。

**(b) OPENING 与 RETURNING 的差值是归因线索。**
147 − 99 = **48ms** 只能来自「进入独有」的工作：播放器创建（H-4）、详情页首次组合、playurl 拉取与首帧解码竞争。这意味着转场优化与 H-4 播放器重建可能耦合——归因时必须把 B 阶段和 D 阶段的边界画清楚（Perfetto 主线程轨道上区分转场自身渲染 vs 播放器启动）。

**(c) 弹幕：窗口化是主收益，免排序是保底收益。**
6 万条全量：3 次排序 + 6 万对象映射 ≈ 50–120ms（静态估算）。窗口化（6 分钟 segment，`SEGMENT_DURATION_MS = 360_000L`）后 n 从 60000 → 数千：即使排序不消除，成本也降一个数量级；再加免排序快路径（上游已保证有序 → 线性验证 O(n) 或直接信任），`setData` 进入个位数 ms。两条腿都要做，顺序为先窗口化（收益大、改动在上层）、后免排序（收益稳、改动在引擎边界）。

**(d) 直播气泡：数量级消除，不是微调。**
10–22 个实时 blur 实例 → 1–3 个（或 0 个，半透明底色替代）。每个实例是一次离屏 RenderEffect pass（F2），减少的是 pass 数本身——这不是「优化」是「取消」。与 H-16 的裸 blur 审计合并为一次系统性收口。

**(e) 行为 bug 使调优失效，必须最先修。**
H-8 门控恒 false 意味着：无论后续把入场动画 spring 调得多快，「快速滚动时不播动画」这一腿从不生效——任何动效调优在这条腿上的投入都是零回报。H-7 同理：用户关掉呼吸后功耗问题依旧。**修 bug 先于调优**是逻辑必然，不是偏好。

---

## 2. 现状盘点（结构化输入文档）

### 2.1 资产（直接复用，不重建）

- 固化测量命令与原始数据（`docs/perf/raw/`，含 10 轮验证过的转场分相位样本）。
- `scripts/perfetto_collect.sh`（三场景长周期 trace，已就绪未用）。
- 转场采集 + 报告 + 门禁（`release_card_transition_sample.sh` + `video_card_transition_report.py`，自带 PASS/FAIL）。
- 8 个 macrobenchmark（含 `BiliPaiDanmakuFrameTimingBenchmark`，与引擎 Trace 埋点 `BiliPaiDanmakuSetData` 对接）。
- Compose 编译器指标开关（`app/build.gradle.kts:349`，接线已确认）。
- 既有 policy 测试体系（`BlurIntensityVisualPolicyTest`、`HomeCardEnterAnimationPolicyTest` 等）+ 新增 A-5 双轨互斥测试。
- 滚动稳态健康（中端 jank 2.67%、平板 2.08%，双双达标）——**首页滚动不是瓶颈**，本计划不投入滚动调优，只设防回归门禁。

### 2.2 债务（按第一性原理分类）

| 类别 | 热点 | 本质 |
|---|---|---|
| 零收益支出（P3，最先修） | H-7、H-8、H-11、H-17、H-22 | 条件写反/写错/无门控，功能上不产生价值的持续消耗 |
| 重复计算（P2） | H-2、H-3、H-18、H-4、H-5 | 排序×3、每帧 new BlurEffect、每次重建 player、5+ 处创建点漂移 |
| 无预算的 GPU 消耗（P5） | H-1、H-6、H-16、H-15 | 全屏逐帧 blur、同屏实例数无上限、裸 blur 绕过入口 |
| 复杂度等级（P4） | H-2/H-3（排序）、H-14（轮询 O(n) 扫描） | O(n log n)/O(n) 本可 O(log n)/事件驱动 |
| 重组扇出（P6） | H-9、H-10 | 根作用域 ~20 个被观察源含高频热源；5 Tab 全常驻 |
| 一致性漂移（快赢） | A-4、H-5 | MotionSpec 三套真相、player 参数五处三种 |

### 2.3 阻塞（P1，硬前置）

GitHub Packages 凭据失效（`gpr.user/gpr.key` 401）→ 所有 `:app` Gradle 任务被阻塞 → macrobenchmark、Compose 指标、A-5 测试运行全部待恢复。恢复动作属用户侧（更新 `~/.gradle/gradle.properties`，classic PAT 需 `read:packages`）。**恢复前只有纯 adb 路径可用**（Perfetto、gfxinfo、分相位采样）——因此 B 阶段（转场归因，依赖 Perfetto）可与 Phase 0 并行启动，不空等。

---

## 3. 量化目标

「必须」= 本计划验收线；「力争」= 需要多项修复叠加或涉及视觉取舍时。

| 指标 | 现状（基线文档） | 必须达成 | 力争达成 | 测量命令（基线 §4） |
|---|---|---:|---:|---|
| 转场 OPENING p50 / p90 | 147.02 / 208.20ms | <33ms / <50ms | <16.7ms（60Hz 预算内） | `release_card_transition_sample.sh` |
| 转场 RETURNING p50 / p90 | 99.31 / 114.61ms | <33ms / <50ms | <16.7ms | 同上 |
| 转场 report.py 门禁 | FAIL（超预算 100%） | 超预算 ≤5%、2× ≤1 | 全绿（含 PSS，按纯转场会话复测） | 同上 |
| `BiliPaiDanmakuSetData` 主线程 | 待测（静态估 50–120ms，6 万条跨窗 seek） | <8ms | <4ms | `BiliPaiDanmakuFrameTimingBenchmark` |
| 弹幕稳态 jank（默认设置） | 待测 | <5% | <2% | 同上 |
| 滚动 jank（中端/平板） | 2.67% / 2.08% ✅ | 维持 <5%（防回归） | — | `mobile_perf_collect.sh` / `tablet_perf_collect.sh` |
| 直播同屏实时 blur 实例 | 10–22 | ≤3 | 1（单 capture 层） | Perfetto GPU 轨道 + 静态审计 |
| 骨架屏「关闭呼吸」后 | 仍有无限动画 + 不查 reduceMotion | 零动画 | — | 代码 + policy 测试 |
| 快滑入场动画门控 | 恒 false（从不生效） | 真实滚动态接入 | — | `HomeCardEnterAnimationPolicyTest` 扩展 |
| mask 开启且无活动帧时 | 每帧全屏 saveLayer + 无条件 invalidate | 零离屏分配 | — | 弹幕 benchmark mask A/B |
| 播放器重建（每次进详情） | 必然发生（时间戳 key） | 同 bvid 重进不重建 | 复用池决策有实测数字 | Perfetto 起播链路分阶段 |

---

## 4. 执行阶段

> 每个任务的格式：**动作 → 证据锚点（file:line）→ 验证 → 退出标准**。
> 阶段内任务按列出顺序执行；阶段间依赖见 §5。

### Phase 0：解除测量阻塞（0.5d，用户侧动作 + 并行准备）

| # | 任务 | 说明 |
|---|---|---|
| 0.1 | 更新 GitHub Packages 凭据 | 用户更新 `~/.gradle/gradle.properties` 的 `gpr.user`/`gpr.key`（classic PAT，`read:packages`）。这是 B 之外所有阶段的前置 |
| 0.2 | 凭据恢复后立即补齐三条基线 | ①`:baselineprofile:connectedReleaseAndroidTest`（弹幕/启动/详情 16 用例）②`:app:compileReleaseKotlin -Pbili.compose.metrics=true`（非 skippable 排行）③运行 A-5 测试 `BiliPaiNavDoubleTransitionExclusivityTest`。数字回写 `baseline-2026Q4.md` §3.3 待补表 |
| 0.3 | 跑一次 `scripts/perfetto_collect.sh --scenario play` | 工具已就绪未执行。起播全链路 + 播放稳态首份 trace，同时服务 B/D 两阶段的归因输入 |

**退出标准**：§3.3 待补基线表全部有数字；A-5 测试绿。
**注**：本阶段不等凭据也可推进 Phase A 与 B1–B2（纯 adb / 纯代码路径）。

---

### Phase A：行为 bug 与快赢（0.5–1d，凭据无关，先行兑现）

原则映射：P3（不执行的代码最快）、P2（消灭重复计算）。这些改动小、收益确定、互不依赖，适合作为独立小切片逐个 commit。

**A1（H-7）骨架屏开关语义反转修正**
- 现场已核实：`HomeFeedSkeletonCard.kt:51-66` —— `rememberSkeletonBreathingEnabled()` 为 true 时走 gentle 脉冲，为 **false（用户关闭）时反而落入另一条 `infiniteRepeatable(tween(2000))`**，且该分支不检查 `rememberSystemReduceMotion()`（该 API 已存在于 `core/ui/motion/ReduceMotion.kt`）。`ContentLoadingSkeletons.kt:52-70` 同模式（tween(1000) shimmer）。
- 动作：①反转开关语义——关闭呼吸 = 静态骨架（pulse 恒定值，无动画）；②所有骨架无限动画统一接 `rememberSystemReduceMotion()`，reduce motion 时定格；③顺带收口骨架脉冲的 4 套时钟（2800/2000/2000/1000ms，A-4 清单项）到 token。
- 验证：新增/扩展 policy 测试（纯 Kotlin：给定 breathingEnabled=false / reduceMotion=true，断言无动画 spec）；`:app:compileDebugKotlin`。
- 退出标准：关闭呼吸后 `dumpsys power` 无持续唤醒；policy 测试绿。

**A2（H-8）入场动画滚动门控接线**
- 现场引用（热点清单静态证据）：`HomeCardEnterAnimationPolicy` 的 `isScrollInProgress` 形参在所有调用点传入的是 `scrollLiteModeEnabled` 且硬编码 false（`VideoCard.kt:508/:630/:701`、`HomeCategoryPage.kt:436/:479`）。
- 动作：把真实滚动态（`LazyGridState.isScrollInProgress` 的 provider）传入调用点；若个别调用点拿不到 state，按既有注释语义降级为「快滑期间整体关闭入场动画」。**先核实每个调用点的 state 可达性再动手**（本计划写作时只核实了策略与测试存在，未逐一核实 5 个调用点）。
- 验证：扩展 `HomeCardEnterAnimationPolicyTest`（已有测试文件）断言滚动中挂载不播动画；真机快滑 3 屏对比 JankStats。
- 退出标准：门控在快滑中真实生效；测试绿。

**A3（H-18）转场 live 路径 BlurEffect 对齐缓存写法**
- 现场已核实：`VideoCardTransitionBackgroundPolicy.kt:768-777`（live 路径）每帧 `BlurEffect(...)` new；同文件 `:312-352`（overlay 路径）与 `:972-981`（`VideoCardTransitionSnapshotLayerState.blurEffect`，带 LRU）已有缓存范式。
- 动作：live 路径复用 `cachedBlurRadiusPx`/`cachedBlurEffect` 局部缓存写法（注意该写法在 `graphicsLayer` 块外的 modifier 局部变量，避免 snapshot state 读写）。
- 验证：`:app:compileDebugKotlin` + 既有 `VideoCardTransitionBackgroundPolicyTest`；转场采样复测确认无回归。
- 退出标准：live 路径不再逐帧分配 BlurEffect（Perfetto 中 `BlurEffect` 相关分配消失或抽样确认）。

**A4（A-4 摘要）MotionSpec 收敛——先补护栏再批量改**
- 依据热点清单快赢项 1：双入场动画系统（app `Animations.kt` Normal=0.70/350 vs ds `AppEntranceMotion` 0.90/380，另有第三套 stagger）、`HomeRefreshMotionSpec` 硬编码 5 组 spring 而 `pullRefreshReleaseSpring` token 存在且被测试锁定却零引用、`CommonListMotionSpec` stiffness 260 vs spatialSpec 380。
- 动作分两步：①**先写「token 消费率」测试**（扫描 feature 层 MotionSpec，断言硬编码时长/刚度必须出现在豁免清单或引用 token——补上 `HardcodedMotionLintTest` 白名单豁免留下的护栏缺口）；②再按孤儿参数清单逐文件收敛到 `AppMotionTokens`，每文件一个 commit。
- 验证：token 消费率测试本身 + `:app:compileDebugKotlin`。
- 退出标准：消费率测试绿且豁免清单为空或每项有注释理由。

---

### Phase B：转场 P0——归因与修复（2–3d，最高用户可感知）

原则映射：F2/P5（像素杠杆）、§1.3(a) 的算术裁决、F6/P7（瞬态可牺牲保真）、P8（循环归因）。

**B1 归因：三层并行的帧内占比（先做，半天）**
- 场景与工具：中端机 120Hz，`scripts/perfetto_collect.sh --scenario manual` 抓 10 轮打开/返回；同时利用已就绪的 `VideoCardTransitionDiagnostics` Perfetto 计数器（`bili.video_card.blur_updates` / `snapshot_records` / `source_layer_draws` / `nav_backdrop_draws`，已核实存在于 `VideoCardTransitionDiagnostics.kt`）区分三层。
- 要回答的三个问题：①冻结景深层 GPU blur 占每帧多少 ms；②飞卡快照（`VideoCardNativeSnapshot.kt:92-99` 每可见卡每帧 `layer.record{}`，H-20）占多少；③NavDisplay transition 合成占多少。④OPENING 独有的 48ms（§1.3(b)）里播放器启动/详情首次组合占多少——这决定 D 阶段（H-4）是否要提前。
- 产出：占比表写入本文件附录，作为 B2 修复顺序的依据。

**B2 修复：按归因占比从大到小逐项出手，每项独立复测**

- **B2a 景深模糊层降采样**（预期主修）：`videoCardTransitionLiveBackgroundEffect` / 冻结层渲染引入 inputScale 思路（对齐 `BlurBudgetPolicy.resolveBlurInputScale`，但转场期可用更激进的档位）：全屏 blur 前先 `graphicsLayer` 缩放到 s（0.5–0.7 区间 A/B），blur 后放大回原尺寸。像素成本 ×s²（F2）：s=0.6 → ×0.36。量化档位（现有 2px/4px 量化）与 radius 保持，避免视觉跳变。注意 `LANDING_COMPRESSION 必须为 0` 的既有约束（上游计划 N-3）不被破坏。
- **B2b 减层/错峰**：检查三层是否真的需要在全部 progress ∈ [0,1] 区间并行绘制。若景深层在 progress > 0.7（detail 已基本就位）后视觉贡献趋零，改为区间外 `renderEffect = null` + `drawContent` 跳过；飞卡快照同理在 LANDING 后停止 record（与 H-20 合并）。
- **B2c 转场期 60Hz 呈现（可选放大器，最后评估）**：仅在 B2a+B2b 后 p50 仍 >16.7ms 时立项。实现倾向 `Surface.setFrameRate(60f)`（经 SurfaceView/无障碍兼容性评估），感知差异由 F6 担保（<300ms 窗口）。**必须 A/B 截图对比 + 预测返回路径回归测试**（预测返回是连续手势，降帧可能被感知为跟手性下降——这是本项的主要风险，若感知不过关则放弃，回到 B2a 加档）。
- **B2c'（并列备选）转场时长与帧内容联合调整**：若 60Hz 化不可行，评估把 291ms 时长内「每帧必须完成的工作」摊薄——例如 OPENING 前 100ms 只跑快卡层、景深延迟淡入（错峰的另一形态）。视觉验收同上。

**B3 验证与回写**
- 命令：`release_card_transition_sample.sh` 全 10 轮 + `video_card_transition_report.py`（相位日志逐轮验证，遵守基线 §4 采样注意事项：每轮验 `phase=OPENING/RETURNING` 计数、checkpoint <0.5s）。
- 复测「纯转场会话」一次（不含播放器启动内存的语义），让 PSS 门禁（≤16MB）生效——基线 §3.1 遗留的语境差异项。
- 触碰 `core/ui/transition/**`、`navigation3/**` 的每个 commit 都要跑 A-5 双轨互斥测试。
- 退出标准：OPENING/RETURNING p50 <33ms（必须）；report.py 门禁超预算/2× 项转绿；A-5 绿。

---

### Phase C：弹幕数据通路（1.5–2d，收益确定性最高）

原则映射：P4（降复杂度等级）、P2（排序一次全链路信任）、P1（benchmark 先行）。

**C1 基线先行**：跑 `BiliPaiDanmakuFrameTimingBenchmark` 全矩阵（默认 / 全开 mask / 最高密度+最大区域 / 2x 倍速，6000 条窗与 6 万条视频），拿到 `BiliPaiDanmakuSetData` 实测数——H-2 目前只有静态估算，先让它变成【实测】再动手。

**C2 窗口化（主收益）**
- 现场已核实：`ByteDanceDanmakuEngine.replaceWindow:100-110` 收到的是上层给的全部 items 再整体 `setData`；热点清单指出入参是「过滤合并后的全部弹幕」（`DanmakuManager.kt:436` `cachedDanmakuList`）而非窗口子集；引擎内已有 segment 概念（`SEGMENT_DURATION_MS = 360_000L`）与 `rollWindowForward`。
- 动作：在 `DanmakuManager` 提交层按 `currentPositionMs` 做窗口裁剪（当前 segment ± 预读一个），只把窗口内 items 传给 `replaceWindow`。跨窗 seek 时换窗口（复用既有 roll/discard 机制），不再全量重灌。
- 验证：benchmark 的 6 万条 seek 用例 + Trace 区段计数；行为等价用既有 `DanmakuPlaybackSyncPolicyTest` 模式补「跨窗后弹幕内容正确」纯 Kotlin 测试。
- 退出标准：`BiliPaiDanmakuSetData` <8ms（6 万条视频跨窗 seek）。

**C3 免排序快路径（保底收益）**
- 现场已核实三重排序：`replaceWindow:103`（`window.items.sortedBy`）→ `buildEngineTimeline:296-298`（map + 再 `sortedBy`）→ `DataManager.setData:50-54`（再 `sortedBy`）。
- 动作：①仓库层（`DanmakuRepository`/`DanmakuManager`）保证 `cachedDanmakuList` 全局有序一次；②`replaceWindow` 去掉 `sortedBy`，改 O(n) `isSorted` 线性验证（不满足才排，防御性且便宜）；③`buildEngineTimeline` 的 mask 与 text 两源各自有序 → 归并或验证后拼接；④`DataManager.setData` 增加信任上游的快路径（同样线性验证兜底）。注意 `appendData` 已有「尾部追加免排」分支（`DataManager.kt:60-66`，已核实），对齐该模式。
- 验证：同 C2；补「乱序输入防御」单测（喂乱序列表断言结果有序）。
- 退出标准：排序次数 3→0（快路径命中时），`BiliPaiDanmakuSetData` 力争 <4ms。

**C4 单条插入归并（H-3）**
- 引用（热点清单静态证据）：`DanmakuManager.kt:2390-2391` 两次全量 `plus().sortedBy()` + `:2397` resync 再走全量路径。
- 动作：二分定位 + 单点插入（O(log n) + memmove），随后只走 `engine.append`（引擎 `append` 路径已是增量，`ByteDanceDanmakuEngine.kt:161-175` 已核实有尾部免排分支）。与 C2/C3 同一 PR 系列内完成。
- 退出标准：发一条弹幕主线程耗时 <8ms（benchmark 连发用例）。

**C5 mask 条件修正（H-11，P3 类，随本阶段顺手修）**
- 引用：`RenderEngine.kt:123-137` saveLayer 条件仅为 `config.mask.enable`；`DanmakuController.kt:252` mask 开启时静止也逐帧重绘。
- 动作：saveLayer 条件改为「mask.enable && 存在活动 MaskData 帧（当前时间 ∈ 某 mask 帧区间）」；无活动帧时恢复静止 pauseInvalidate。
- 验证：benchmark mask A/B（开 mask 播无 mask 内容的视频，帧耗时应与 mask 关闭一致）。
- 退出标准：mask 开启无活动帧时零离屏分配。

**C6（P2 长尾，可选）**：H-12（每帧 5 层聚合重排缓存——绘制集合仅进出屏变化）、H-13（DrawCachePool 容量 8 → ~64）、H-14（高级弹幕 16ms 轮询改 `withFrameNanos` + filter 结果按列表版本缓存）。按 C1 基线数字决定是否立项，避免无数据支撑的盲改。

---

### Phase D：播放器生命周期收敛（2d，先测后改）

原则映射：P2（重复计算）、P1（§4.4 判定要点原文：先测重建成本再决定）、P8。

**D1 归因**：Perfetto 起播链路分阶段（转场 → attachPlayer → loadVideo → prepare → 首帧），量化「每次进详情重建 ExoPlayer」的实际成本（H-4 现场已核实：`VideoPlayerState.kt:994` 时间戳作 `playerCreationKey`，`:992-993` 注释自证是修「重复打开同一视频无声音」的权宜之计；`:999` remember(bvid, key)）。同时量化 §1.3(b) 的 OPENING 独有 48ms 中播放器占比。

**D2 根因修复，再移除时间戳 key**
- 顺序不可颠倒：先复现并理解「重复打开同一视频无声音」的真实根因（大概率是释放/持有生命周期窗口：`DisposableEffect :1144-1175` 延迟 release 与快速连续进出的窗口期、或 `shouldSuspendLocalPlaybackWhenSessionInactive` 与小窗交还的交互），为它写一个可复现测试或诊断；然后修根因；最后移除时间戳 key，让同 bvid 重进复用 player。
- 验证：`:app:compileDebugKotlin` + 针对生命周期决策的纯 Kotlin policy 测试（`shouldReuseMiniPlayerAtEntry` / `shouldSuspendLocalPlaybackWhenSessionInactive` 已是可测函数，扩展场景矩阵）；真机「快速进出同一视频 ×10」声音/画面回归。
- 退出标准：同 bvid 重进不重建 player（Perfetto 确认）；无声音 bug 不回归；起播 p50 改善数字回写。

**D3 PlayerFactory 收敛（H-5，结构修复）**
- 引用（热点清单静态证据）：三处主创建点（`VideoPlayerState` / `MiniPlayerManager.kt:1805` / `PortraitVideoPager.kt:648`）参数漂移——竖屏滑动页无媒体缓存工厂、无 LoadControl 限流、无 becomingNoisy/wakeMode；`setShowBuffering` 五处三种配置；另 `BasePlayerViewModel.kt:254/:328`、`BangumiPlayerScreen.kt:178`、`VideoPlaybackUseCase.kt:1227/:1255` 各自手搓。**H-21 一并处理**：硬解分支补 `setEnableDecoderFallback`（健壮性）。
- 动作：新建 `core/player/PlayerFactory`（唯一创建点），行为差异（竖屏页要不要缓存工厂等）以显式参数或预置 profile 表达，差异点集中可见；顺带 V-5 反向偏差修正。
- 验证：`:app:compileDebugKotlin` + 竖屏页/详情页/小窗三条路径真机冒烟（`scripts/release_smoke_gate.sh` 改动面大时手动跑，遵守「不主动 assemble」政策——该项若需打包，用 `:app:installDev`）。
- 退出标准：全仓库 `ExoPlayer.Builder` 调用点 ≤1 处（工厂内部）；漂移表逐项消除或显式化。

**D4（P2 长尾）**：H-17 诊断路径惰性化——`VideoPlayerState.kt` 回调与 `loadVideo` 入口的字符串拼接改惰性 lambda、诊断开关前置判断、`Logger.w` 接入门禁策略（`Logger.kt:361-376`）。独立小切片，可提前穿插。

---

### Phase E：液态玻璃预算体系（1.5–2d，GPU 归因驱动）

原则映射：P5（pass 数量级消除）、F2、P9（建立同屏预算这一「制度」而非逐点修）。

**E1 直播气泡（H-6，极值场景，数量级收益）**
- 引用：`LivePortraitChrome.kt:374` 每条可见消息气泡独立挂裸 `hazeEffectCompat`（items @:184），同屏 10–22 实例；`LivePlayerScreen.kt:1431`、`LivePortraitChrome.kt:151/:704` 另有裸点（H-23）。
- 动作（二选一，视觉验收决定）：①改「单 capture 层 + 逐条低级特效」（气泡区域共享一次背景捕获）；②直接降级为半透明底色（气泡小、blur 视觉收益低——倾向此案起步，因为它把 pass 数从 10–22 直接归零，符合 P5「取消优于优化」）。**这是视觉语言变化，做成可切换并留截图对比，供最终拍板**。
- 验证：Perfetto GPU 轨道前后对比（pass 数、GPU 频率驻留）；直播间长弹幕场景 gfxinfo。
- 退出标准：直播间同屏实时 blur ≤3。

**E2 同屏实例预算制度（G-1 的立项落地）**
- 动作：①静态普查表复核（热点清单已有场景 × 实例 × 半径表）；②在 `UnifiedBlur`/`BlurBudgetPolicy` 层引入同屏实例计数与预算上限（如 HEADER+BOTTOM_BAR+OVERLAY 总实时实例 ≤N，超限按 surfaceType 优先级降级为纯色底）；③`forceLowBlurBudget` 字段接通到页面内容（H-10 对冲不完整项，与 F 阶段 H-10 的 A/B 联动）。
- 验证：`design-system` 既有 `BlurIntensityVisualPolicyTest` 模式补「预算路由」单测；中端机首页/动态/评论 sheet 三场景 Perfetto。
- 退出标准：预算策略有测试锁定；首页极值（≈14 实例）场景 GPU 帧耗时给出前后数字。

**E3 裸 blur 收口（H-16 + H-23，与 E1 合并审计）**
- 约 15 组裸 blur 路径 + 直播 3 点，逐点核对路由到 `unifiedBlur` 统一入口；`MusicPlayerContent` 逐行歌词/评论线程的返回覆盖模糊（逐帧重建）优先处理。
- 退出标准：裸 `hazeEffectCompat`/裸 `BlurEffect` 调用点清单为空或全部有豁免注释。

**E4 滚动启停翻转（H-15）**：`UnifiedBlur.kt:111-155` 滚动启停瞬间 inputScale None↔Fixed(0.82-0.88) 翻转重建配置——固定 inputScale 或对齐两档常量避免参数重算。fling 起/停各 20 次 gfxinfo framestats 验证尖峰消除。

---

### Phase F：重组与常驻开销（1–1.5d，回归风险最高，放后）

原则映射：P6（状态作用域 = 扇出半径）。

**F1（H-9）高频状态下放**：`AppNavigation.kt:1487-1489` 的 `downloadTasks`（下载进度流）与 audio playlist/active 高频更新触发根重组。动作：状态下放到消费点（下载进度只让下载指示器重组）、流上加 `distinctUntilChanged`/节流。**改动前先跑 Compose 编译器指标（Phase 0.2 产出）定位 AppNavigation 实际重组扇出**，用数据选刀口；每步跑 A-5 测试防导航回归。退出标准：下载进行中 NavDisplay 宿主无周期性重组（Perfetto recompose 轨道或 Layout Inspector 计数）。

**F2（H-10）beyondViewportPageCount A/B**：临时构建 0 vs 4 两组，对比冷启动后内存与首切 Tab 帧耗时（上游计划 §6.3-3 固化方法）。**用数字决定保留或改 1–2**——两种结论都入档（「收敛省 X MB」或「现状合理，依据如下」），不许拍脑袋。与 E2 的 `forceLowBlurBudget` 接通联动。

**F3（H-14，若 C6 未覆盖）**：高级弹幕轮询改造 + `remember` key 稳定化。

---

### Phase G：门禁固化与收尾（1d，P9：性能是熵）

| # | 动作 | 说明 |
|---|---|---|
| G1 | 转场分相位门禁进 CI | 每晚 + 触碰 `core/ui/transition/**`、`navigation3/**` 的 PR 触发；阈值沿用 report.py 自带（超预算>5、2×>1、PSS≤16MB）+ 转场相位 p50 <33ms 新基线线 |
| G2 | 滚动门禁进 CI | `mobile_perf_collect.sh` 每晚；jank<5%、p99<2× 帧预算 |
| G3 | 弹幕门禁 | 触碰 `danmaku-engine/**`、`feature/video/danmaku/**` 的 PR 跑 `BiliPaiDanmakuFrameTimingBenchmark`；`BiliPaiDanmakuSetData` <8ms、稳态 jank<5% |
| G4 | 既有守护保持 | policy 测试（每 PR）、A-5 双轨互斥（每 PR）、`RuntimeVisualGuardTracker` 遥测阈值 <1 次/小时（线上） |
| G5 | 长尾清账 | H-19（prefetch 注册表增量维护 + 手势首帧预热）、H-20（若 B2b 未覆盖）、H-22（`Log.d` 惰性）按剩余预算清 |
| G6 | 全量复测 + 基线文档改版 | 全部修复后按基线 §4 命令跑一轮完整矩阵，`baseline-2026Q4.md` 升级为「优化后基线」，所有「待补/待测」项清零或明确标注原因；低端真机降级路径（设备到位后）补测 |

---

## 5. 依赖关系与关键路径

```
Phase 0（凭据）────────────┬────────────→ C1 benchmark、F1 Compose 指标
Phase A（快赢，无依赖）────→ 独立完成，随时可做
Phase B1 归因（Perfetto，无依赖）→ B2 修复 → B3 复测
                                   │
                    B1 的 ④ 播放器占比 ──→ D1（可提前触发 D 阶段）
Phase 0.3（play 场景 trace）───────→ B1 / D1 共用
C2 → C3 → C4 → C5（C1 数字决定 C6 是否立项）
D1 → D2 → D3（D2 根因不修复不得移除时间戳 key）
E1 → E2 → E3 → E4（E1 视觉验收可并行推进）
F1 依赖 Phase 0.2 的 Compose 指标；F2 依赖 E2 的 forceLowBlurBudget 接通
G 依赖全部前序退出标准
```

关键路径：**B1 → B2 → B3 →（若 48ms 归因指向播放器）D1 → D2 → G**。转场是唯一 P0，其他阶段可与 B 并行推进（不同人/不同会话可同时开 A 与 B1）。

## 6. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| H-4 移除时间戳 key 复活「无声音」bug | 用户可感知回归 | D2 强制「先根因、先测试、后移除」顺序；移除单独成 commit 便于回滚 |
| 转场 60Hz 化被感知为跟手性下降（预测返回） | 体验回归 | B2c 仅作最后放大器；A/B 感知验收不过即放弃（§1.3(a) 已证明它本就不可能是主修） |
| 弹幕窗口化引入跨窗内容缺失/重复 | 功能回归 | 行为等价纯 Kotlin 测试（跨窗后内容正确、乱序防御）；`DanmakuPlaybackSyncPolicyTest` 模式扩展 |
| 直播气泡降级改变视觉语言 | 违反「保留视觉语言」默认 | 做成可切换 + 截图对比供拍板；半透明底色仅在气泡这种「小面积、低 blur 收益」场景起步 |
| AppNavigation 改动引发导航回归 | 高风险区域（4739 行） | 每步跑 A-5；用 Compose 指标定位刀口而非盲改；小步 commit |
| 优化后瓶颈转移导致某项收益低于预期 | 计划偏差 | P8：每阶段「归因 → 修复 → 复测 → 再归因」，占比表驱动下一刀；不批量盲改 |
| 测量环境噪声（feed 位移、ring 冲刷、Python stub） | 数据无效 | 严格遵守基线 §4 采样注意事项三条（相位日志逐轮验证、checkpoint <0.5s、Python shim） |
| Kotlin daemon / 基础设施故障误判为产品回归 | 时间浪费 | 遵守 AGENTS.md 验证卫生：基础设施错误立即切换确定性验证路径并显式报告区别 |

## 7. 复测与回写制度（基线治理）

1. **每个修复切片完成后立即复测**，命令 = 基线 §4 固化命令，不做任何「顺手多测」。
2. **数字回写 `baseline-2026Q4.md`**：在对应表格新增「优化后」列或追加日期小节，附 raw 数据文件名（`docs/perf/raw/` 命名沿用现有惯例）。基线是对照系，系统状态改变后必须重新校准（F1 的直接推论）。
3. **热点清单状态机**：`hotspots-2026Q4.md` 每条热点标注状态（静态 → 实测确认/证伪 → 修复中 → 已修复+复测数字 → 关闭）。「证伪」也是有效产出（如 H-21 的软解误选已证伪），同样入档。
4. **每个切片 commit + push**（AGENTS.md），commit 信息不含 AI 署名。
5. 出现与基线矛盾的测量结果时，先怀疑测量语境（设备、变体、场景、采样时机），再怀疑代码——本轮 v1/v2 无效样本的教训（基线 §3.1 原始数据注释）已证明这一顺序的正确性。

## 8. 里程碑与工作量

| 里程碑 | 内容 | 工作量 | 交付判据 |
|---|---|---|---|
| M0 | Phase 0 + Phase A | 1–1.5d | 凭据恢复、三条基线补齐、A1–A4 全绿并 push |
| M1 | Phase B 转场 | 2–3d | OPENING/RETURNING p50 <33ms，门禁超预算项转绿，占比表入档 |
| M2 | Phase C 弹幕 | 1.5–2d | `setData` <8ms，mask 无活动帧零分配 |
| M3 | Phase D 播放器 | 2d | 同 bvid 不重建；PlayerFactory 单点；起播数字回写 |
| M4 | Phase E 玻璃 | 1.5–2d | 直播 ≤3 实例；预算制度 + 测试锁定 |
| M5 | Phase F + G | 2–2.5d | 重组扇出数字、A/B 结论入档；CI 门禁上线；基线文档改版 |

总计 **10–12 个工作日**（单人串行；A/B1/E1 可并行则压缩至 ~8）。每完成一个里程碑即是一个可回滚、可汇报的切片。

---

## 附：任务 → 热点 → 文件索引

| 任务 | 热点 | 主文件（证据锚点） | 类别（§1.2） |
|---|---|---|---|
| A1 | H-7 | `HomeFeedSkeletonCard.kt:51-66`、`ContentLoadingSkeletons.kt:52-70` | 零收益支出 |
| A2 | H-8 | `HomeCardEnterAnimationPolicy.kt`、`VideoCard.kt:508/:630/:701`、`HomeCategoryPage.kt:436/:479` | 零收益支出 |
| A3 | H-18 | `VideoCardTransitionBackgroundPolicy.kt:768-777`（对齐 :312-352） | 重复计算 |
| A4 | A-4 | `Animations.kt`、`AppMotionTokens.kt`、33 文件清单（见热点清单） | 一致性漂移 |
| B2a/b/c | H-1/N-3/H-20 | `VideoCardTransitionBackgroundPolicy.kt`、`VideoCardNativeSnapshot.kt:92-99` | GPU 预算 |
| C2/C3/C4 | H-2/H-3 | `DanmakuManager.kt:436/:2390-2397`、`ByteDanceDanmakuEngine.kt:103/:296-298`、`DataManager.kt:50-54` | 复杂度/重复计算 |
| C5 | H-11 | `RenderEngine.kt:123-137`、`DanmakuController.kt:252` | 零收益支出 |
| C6 | H-12/H-13/H-14 | `RenderEngine.kt:116-121`、`ScrollLayer.kt:47`、`DrawCachePool.kt:43-62`、`AdvancedDanmakuOverlay.kt:62-100` | 复杂度 |
| D2 | H-4 | `VideoPlayerState.kt:994/:999/:992-993/:1144-1175` | 重复计算 |
| D3 | H-5/H-21 | 三创建点 + `MiniPlayerManager.kt:1805`、`PortraitVideoPager.kt:648`、`BasePlayerViewModel.kt:254/:328`、`BangumiPlayerScreen.kt:178`、`VideoPlaybackUseCase.kt:1227/:1255` | 一致性漂移 |
| D4 | H-17 | `VideoPlayerState.kt:589/:647/:686/:817`、`Logger.kt:361-376` | 零收益支出 |
| E1/E3 | H-6/H-23/H-16 | `LivePortraitChrome.kt:184/:374/:151/:704`、`LivePlayerScreen.kt:1431` | GPU 预算 |
| E2 | G-1/H-10 对冲 | `BlurBudgetPolicy.kt`、`UnifiedBlur.kt` | GPU 预算（制度） |
| E4 | H-15 | `UnifiedBlur.kt:111-155` | GPU 预算 |
| F1 | H-9/N-2 | `AppNavigation.kt:1487-1489` 等 | 重组扇出 |
| F2 | H-10/N-1 | `AppTopLevelNavigationPolicy.kt:33/:212-218` | 重组扇出 |
| G5 | H-19/H-22 | `HomeCoverReturnPrefetch.kt:36-46`、`DanmakuManager.kt:1349-1352` | 长尾 |
