# BiliPai 全面性能审查计划

> 状态：已执行（2026-10-01 第一轮，静态审计全量完成 + 中端/平板真机实采；macrobenchmark 与 Compose 指标因 GitHub Packages 凭据阻塞待补）｜ 编写日期：2026-09-30
> 产出：`docs/perf/baseline-2026Q4.md`（基线）、`docs/perf/hotspots-2026Q4.md`（热点清单与门禁建议）、`scripts/perfetto_collect.sh`、A-5 双轨互斥守护测试
> 覆盖维度：视频播放、弹幕滚动、页面切换、液态玻璃（模糊）、UI 动效
> 原则：先建立基线，再按维度归因；每个结论必须有测量数据支撑，静态审查疑点一律标记为「待验证」。

---

## 0. 背景与目标

本项目已具备相当完整的性能工具链（8 个 macrobenchmark、gfxinfo 分相位采集脚本、JankStats 生产接线、Compose 编译器指标开关），但存在三个问题：

1. **基线缺失**：`app/src/main/baseline-prof.txt` 不存在，Baseline Profile 链路（生成器 + 脚本 + ProfileInstaller）闭环但产物未提交，启动/滚动优化可能未实际生效。
2. **工具未系统化使用**：现有 benchmark 与脚本没有形成可对比的基线数字，性能回归靠体感。
3. **已知架构热点未经量化**：静态走查发现一批可疑点（见各维度「重点疑点」），需要测量确认是否真为瓶颈、优先级如何。

本次审查目标：

- 建立五个维度的**量化基线**（指定设备 + 指定场景 + 固化命令）。
- 对每个维度输出**热点清单**（P0/P1/P2），每条带测量证据与修复建议。
- 产出**可重复执行的性能门禁**建议（哪些指标进 CI、阈值多少）。

不在范围内：APK 体积、网络请求性能（除非直接影响首帧）、插件系统性能、构建性能。

---

## 1. 设备矩阵与环境

| 档位 | 代表设备 | 用途 |
|---|---|---|
| 高端真机 | 近两年旗舰（如 8 系/天玑 9 系） | 基线数字的「上限」参照，验证高端不掉帧 |
| 中端真机 | 中端 SoC + 8GB | 主要决策设备，多数结论以此为准 |
| 低端真机 | 4GB RAM、Android 10–12（API 29–31） | 降级路径验证（RenderEffect 需 S+、runtime shader 需 T+）、低端体验底线 |
| 平板 | 现有 `tablet_perf_collect.sh` 支持的设备 | 大屏布局 + MotionTier.Enhanced 分档验证 |
| 模拟器 | pixel6Api31（ManagedVirtualDevice，已配置） | macrobenchmark 可重复性参照，不作为真机结论依据 |

环境要求：

- 真机测量统一使用 `release` 或 `dev` 变体（`./gradlew :app:installDev`），**禁止 debug/smooth 变体做性能结论**（AGENTS.md 打包政策；smooth 无 R8，数字不可信）。
- macrobenchmark 统一跑 `./gradlew :baselineprofile:connectedReleaseAndroidTest`（AGENTS.md 已授权路径）。
- 每次采集前：固定亮度、关闭开发者选项动画缩放（真机手动场景）、杀后台、充电状态一致。
- 原始数据统一落盘 `docs/perf/raw/`（现有脚本已按此约定）。

---

## 2. 方法论与工具链

### 2.1 已有工具（直接复用）

| 工具 | 路径 | 用途 |
|---|---|---|
| 8 个 macrobenchmark | `baselineprofile/src/main/kotlin/.../baselineprofile/` | 启动（4 种 CompilationMode）、首页滚动、底部 pager、视频详情转场（16 个用例）、弹幕（帧计时+内存+Trace）、非直播 Surface、设置返回 |
| 卡片转场采集 + 报告 | `scripts/release_card_transition_sample.sh` + `scripts/video_card_transition_report.py` | 分相位（OPENING/RETURNING/Predictive*）帧统计，输出 p50–p99，自带门禁（超预算>5、2×超预算>1、PSS 增长>16MB 判失败） |
| 信息流实采 | `scripts/mobile_perf_collect.sh` / `tablet_perf_collect.sh` | 真机 gfxinfo + meminfo |
| 首页/转场手动采样 | `scripts/release_home_scroll_sample.sh`、`card_transition_gfxinfo.sh` | release 包低扰动采样 |
| JankStats + RuntimeVisualGuardTracker | `MainActivity.kt`、`core/ui/performance/RuntimeVisualGuardTracker.kt` | 生产内 jank 观测与 MotionTier 降级决策流 |
| Compose 编译器指标 | `-Pbili.compose.metrics=true` | 无设备即可量化 skippable/restartable、组合轨迹 |
| 弹幕引擎 Trace 埋点 | `ByteDanceDanmakuEngine.kt:327-337`（`BiliPaiDanmakuSetData/Append`） | 与 macrobenchmark TraceSectionMetric 对接 |
| 播放诊断环形日志 | `VideoPlayerState.appendDiagnosticEvent`（:532-544） | 起播链路、丢帧、首帧时间的应用侧记录 |

### 2.2 需要补充的工具（本计划 Phase 0 一并搭好）

1. **验证 baseline profile 实际生效**：安装 dev 包后 `adb shell dumpsys package com.android.purebilibili | grep -i profile`（或 `cmd package compile` 相关输出），确认 dex2oat profile 已安装（`Dex2OatProfileInstallPolicyTest` 守卫的策略在真机上的表现）。
2. **Perfetto 长周期采集脚本**（缺口）：`scripts/perfetto_collect.sh`，覆盖「播放 10 分钟 + 弹幕全开」「切 5 个 Tab 往返」等宏场景，抓主线程、RenderThread、GPU 频率、GC。现有 fullTracing 仅在 benchmark 内部生效。
3. **基线数字汇总表**：`docs/perf/baseline-2026Q4.md`，每个维度一张表（场景 × 设备 × p50/p90/p95/p99/jank%），后续所有优化以它为对照。

### 2.3 归因流程（每个疑点统一走四步）

1. **静态**：代码走查 + `-Pbili.compose.metrics=true` 报告确认 skippable/稳定性。
2. **复现**：用对应 macrobenchmark 或脚本在参照设备上拿到具体数字。
3. **归因**：Perfetto/Trace 区段确认时间花在哪个线程哪个阶段；区分 主线程 CPU / RenderThread / GPU / GC。
4. **判定**：对照阈值（§8）分级 P0–P2，写入热点清单；不确定的明确标注「证据不足」。

---

## 3. Phase 0：基线建立（先于一切维度，约 0.5–1 天）

这是整个计划的前置，不做完不进入维度审查。

| # | 动作 | 命令/方式 | 产出 |
|---|---|---|---|
| 0.1 | **验证 baseline profile 是否生效**（发现于基础设施盘点，最高优先） | `update_baseline_profile.sh` 生成并提交 `app/src/main/baseline-prof.txt`；真机验证 profile 安装 | 结论写入基线文档；若未生效，此项本身就是 P0 修复项 |
| 0.2 | 跑全部 8 个 macrobenchmark | `:baselineprofile:connectedReleaseAndroidTest`（模拟器 pixel6Api31）+ 至少一台中端真机抽查 | 各场景 FrameTiming/Startup 基线数字 |
| 0.3 | 生成 Compose 编译器指标 | `./gradlew :app:compileReleaseKotlin -Pbili.compose.metrics=true` | 非 skippable composable 排行，作为重组审查输入 |
| 0.4 | 采集低端真机降级路径行为 | 手动滚动 + 播放，读取 RuntimeVisualGuard 日志（jank≥7.5% 触发降档、60s 冷却） | 确认降档是否频繁触发（频繁=体感劣化的直接证据） |
| 0.5 | 补 `scripts/perfetto_collect.sh` | 新脚本 | 长场景系统级 trace 能力 |

---

## 4. 维度一：视频播放

### 4.1 代码地图

- 播放器状态与创建：`app/.../feature/video/state/VideoPlayerState.kt`（`rememberVideoPlayerState` :999 创建、DisposableEffect :1144-1175 延迟 release）
- 小窗单例：`app/.../feature/video/player/MiniPlayerManager.kt`（`ensurePlayer` :1849）
- 竖屏滑动页：`app/.../feature/video/ui/pager/PortraitVideoPager.kt`（:650）
- 渲染输出：`app/.../feature/video/ui/section/VideoPlayerSection.kt`（:3621-3706 SurfaceView/TextureView 切换）、`VideoOutputRouter.kt`（Anime4K 接管）
- 缓冲策略：`VideoPlayerState.kt:91-109,143`（`FirstQuarterAwareLoadControl`）
- 解码器工厂：`app/.../core/player/HiResCompatibleRenderersFactory.kt`、`dolby-ffmpeg-decoder/`（eac3、arm64-v8a）
- 起播链路：`AppNavigation.kt:2983` → `VideoDetailEntryLoadPolicy.kt`（转场门控）→ `VideoPlaybackViewModel.loadVideo`（:2979）→ `VideoPlaybackUseCase`（:407，详情+playurl 并行）

### 4.2 重点疑点（静态走查发现，全部待验证）

| 编号 | 疑点 | 位置 | 风险 |
|---|---|---|---|
| V-1 | **每换一个 bvid 完整重建 ExoPlayer**：`remember` key 含 `System.currentTimeMillis()`，OkHttp factory/cache 包装/renderers 全部重建 | VideoPlayerState.kt:994-999 | 起播延迟、内存抖动；需确认是否刻意为之（复用判定在 `shouldReuseMiniPlayerAtEntry` :205） |
| V-2 | ExoPlayer 创建逻辑在 3 处复制（详情/小窗/竖屏 pager），参数可能漂移；`setShowBuffering` 两处配置不一致 | 三处创建点、FullscreenPlayerOverlay/MiniPlayerOverlay | 行为不一致 + 修复难同步 |
| V-3 | Player.Listener 回调在诊断关闭时仍做字符串插值并整对象替换 `PlaybackDebugInfo`（StateFlow 重建） | VideoPlayerState.kt:318-460, 532-544, 578-600 | 高频事件主线程开销 + 重组扇出 |
| V-4 | `shouldContinueLoading` 每次 buffer 决策做 `timeline.getPeriodByUid` + runCatching | VideoPlayerState.kt:178 | 低频但位于缓冲热路径 |
| V-5 | 始终 `EXTENSION_RENDERER_MODE_ON`，软解 FFmpeg 可能在硬解可用时被误选 | VideoPlayerState.kt:1044 | 低端机软解 4K 掉帧风险 |
| V-6 | 同一 player 在 inline/全屏/小窗三个 PlayerView 间换绑，`key(...)` 整体重建 PlayerView 导致 surface 重走 attach（注释自述「解决黑屏」） | VideoPlayerSection.kt:3635-3687 | 转场/全屏切换黑屏与掉帧 |
| V-7 | `loadVideo` 入口大量同步状态判定 + 主线程 Logger 字符串拼接 | VideoPlaybackViewModel.kt:3081 附近 | 起播主线程抖动 |

### 4.3 测量方案

1. **起播全链路**（点击卡片 → 首帧渲染）：Perfetto trace + 应用内诊断环（`applyRenderedFirstFrameDebugInfo` :392），分阶段计时：转场动画 → `attachPlayer` → `loadVideo` → `prepare` → first frame。场景：冷启动首播、热起播、换 P（同 bvid 换分P，验证 V-1 影响）。目标：把「转场让路」门控（`VideoDetailEntryLoadPolicy`）的实际等待画出来，判断门控时长是否合理。
2. **播放稳态**：30 分钟连续播放（中端机，弹幕关/开两组），采样 `dumpsys meminfo` PSS 曲线 + media3 `DecoderCounters` 丢帧数（`droppedBufferCount`/`maxDroppedBufferCount`，可经诊断环或临时 debug 包输出）。
3. **换绑/重建开销**（V-6）：进全屏、退全屏、进出小窗各 20 次，gfxinfo framestats 统计黑屏帧数与 >2× 预算帧。
4. **解码路径**（V-5）：同一高码率视频在「硬解可用」与「强制 FFmpeg」下对比丢帧与功耗；Dolby（eac3）片源单独验证软解路径在低端机表现。
5. **内存**：SimpleCache 容量行为 + 10 个视频连续进出后的 PSS 与 LeakCanary（`-Pbili.debug.leakCanary=true` debug 包）。

### 4.4 判定要点

- 起播各阶段耗时分布（首帧目标先测基线再定阈值，不拍脑袋）。
- 稳态播放 jank<1%（播放器 SurfaceView 帧不进 gfxinfo 普通统计，以 DecoderCounters + Perfetto 为准）。
- 换 bvid 是否重建：给出「重建成本实测值」，再决定是否值得做 player 复用池。

---

## 5. 维度二：弹幕滚动

### 5.1 代码地图

- 引擎（vendored bytedance/DanmakuRenderEngine，已 Kotlin 化）：`danmaku-engine/src/main/java/com/bytedance/danmaku/render/engine/`
  - 主循环：`DanmakuController.kt:240-273`（每帧 draw + `postInvalidateOnAnimation` 自续）
  - 排序/绘制：`RenderEngine.kt:121-170`
  - 行管理：`render/layer/line/ScrollLine.kt`、`ScrollLayer.kt`
  - 对象池：`render/cache/DrawCachePool.kt`（容量固定 8）
- App 适配层：`danmaku-engine/src/main/java/com/android/purebilibili/danmaku/engine/`（`ByteDanceDanmakuEngine.kt`、`DanmakuRenderView.kt`、`ReverseScrollLayer.kt`）
- 业务层：`app/.../feature/video/danmaku/DanmakuManager.kt`（加载/漂移同步 :1307-1358、窗口提交 :1770-1843）、`DanmakuRepository.kt`、`DanmakuMerger.kt`、`DanmakuPlaybackSyncPolicy.kt`
- 高级弹幕（Mode 7/9，另一条 Compose 路径）：`AdvancedDanmakuOverlay.kt`
- 挂载点：`VideoPlayerSection.kt:4389-4437`

### 5.2 重点疑点

| 编号 | 疑点 | 位置 | 风险 |
|---|---|---|---|
| D-1 | **窗口提交在主线程做三次全量排序/映射**：`replaceWindow` → `buildEngineTimeline`（map + 2 次 sort）→ `DataManager.setData` 再 sort，`Dispatchers.Main.immediate` 执行 | ByteDanceDanmakuEngine.kt:100-110, 296-298；DataManager.kt:50-54 | 6 万条弹幕视频拖进度条跨窗时卡主线程（疑似 P0） |
| D-2 | 每帧全量 `sortWith` + `ScrollLayer.getPreDrawItems` 每帧重建 LinkedList | RenderEngine.kt:121；ScrollLayer.kt:104-110 | 稳态每帧 CPU |
| D-3 | `DrawCachePool` 容量固定 8，超出即走工厂新建 | DrawCachePool.kt:38-62 | 高密度场景帧内分配 → GC |
| D-4 | mask（智能防遮挡）开启即每帧全屏 `saveLayer` 离屏合成 | RenderEngine.kt:123-137 | 单项最大 GPU 开关 |
| D-5 | 每条弹幕 2 次 `drawText`（描边 pass）、每 DrawItem 常驻 2 个 Paint | TextDrawItem.kt:38-39, 76-98 | 稳态 GPU/CPU |
| D-6 | 高级弹幕 Compose 路径 `delay(16)` 轮询 + 每 tick 全量 `any/filter` 扫描 | AdvancedDanmakuOverlay.kt:62-100 | BAS 多时每帧 O(n)×2 + 重组压力 |
| D-7 | 漂移同步每 tick 无条件 `Log.d` | DanmakuManager.kt:1349-1352 | 低频，量小 |

### 5.3 测量方案

现有 `BiliPaiDanmakuFrameTimingBenchmark` 已覆盖 6000 条/窗、6 万条/视频、连续 seek、直播突发，并带 `MemoryUsageMetric` + Trace 区段计数，直接作为主测量工具：

1. **基线**：跑该 benchmark（弹幕设置矩阵至少 4 组：默认 / 全开 mask / 最高密度+最大显示区域 / 倍速 2x），记录 FrameTiming + GC% + `BiliPaiDanmakuSetData` 耗时。
2. **D-1 归因**：Perfetto 抓「6 万条视频 seek 跨 3 个窗口」场景，测 `setData` 在主线程的实际阻塞毫秒数。
3. **帧内分配**：benchmark 内存指标 + 针对性 heap dump，验证 D-3（统计 DrawItem 工厂新建次数，可临时加计数器）。
4. **mask 开销**（D-4）：mask 开/关 A/B 帧计时，低端机单独跑（GPU 弱，差异放大）。
5. **高级弹幕**（D-6）：构造 50+ 条 BAS 场景，JankStats + recomposition 统计。
6. **设置联动**：`viewportScale`/config 变更触发全量重排（`remeasureData` :84-97、`onConfigChanged` :215-232），验证用户拖设置滑杆时的卡顿。

### 5.4 判定要点

- 稳态（默认设置、中端机）：jank<5%，无 >3× 帧预算的尖峰。
- `BiliPaiDanmakuSetData` 主线程耗时 <8ms（约半帧）；超即 D-1 立 P0。
- mask 开启的帧耗时增幅（量化后决定是否做 mask 降采样/降频）。

---

## 6. 维度三：页面切换与转场

### 6.1 代码地图

- 导航壳：Miuix NavDisplay（Nav3 风格）——`navigation3/BiliPaiNavDisplayHost.kt:676-693`、`navigation3/BiliPaiNavKey.kt`；策略层 `navigation/AppNavigation.kt`（4739 行）
- 底部 Tab：`HorizontalPager`（AppNavigation.kt:2284）+ `MainBottomPagerState`（navigation/MainBottomPagerState.kt:53-79）+ `AppTopLevelNavigationPolicy.kt`（`beyondViewportPageCount` :33, :212-218）
- 卡片→播放页转场（自研三层）：冻结源卡 `VideoCardTransitionSession.kt:35-77` + host 景深层 `VideoCardTransitionHostDepthLayer.kt`（GraphicsLayer+BlurEffect，blur≤12dp、2px/4px 量化，`VideoCardTransitionBackgroundPolicy.kt:46-49`）+ 飞卡 `VideoCardNativeSnapshot.kt`、单时钟 `VideoCardTransitionClock.kt`、返回时间线 `VideoCardReturnTimeline.kt`
- 防双层动画：`navigation3/BiliPaiNavContentTransformPolicy.kt:30-36`（Compose ContentTransform 显式 None）
- 预测返回：`navigation3/predictiveback/`（AOSP/MIUIX/Classic/Scale 多风格）

### 6.2 重点疑点

| 编号 | 疑点 | 位置 | 风险 |
|---|---|---|---|
| N-1 | `beyondViewportPageCount = 页数-1`：首帧后 5 个 Tab 全常驻组合（含各自 blur chrome） | AppTopLevelNavigationPolicy.kt:33, :212-218 | 内存 + 首切 Tab 布局成本；作者已用切页低模糊预算对冲（:220-226），需量化该取舍 |
| N-2 | AppNavigation.kt 单 composable 收集几十个 settings state，任一变化大范围重组 | AppNavigation.kt | Tab 切换/设置变更时的重组风暴 |
| N-3 | 转场三层并行（NavDisplay transition + 飞卡 + 景深模糊），单时钟同步，注释要求 LANDING_COMPRESSION 必须为 0 | MiuixVideoCardNavTransition.kt 等 | 任一层漂移=视觉 pop；冻结层逐帧 BlurEffect GPU 成本 |
| N-4 | 返回时 previousScene 重挂载 + 封面解码并发（已有 `maybePrefetchHomeCoversForVideoReturn` 缓解 :1341） | AppNavigation.kt | 返回掉帧；需验证 prefetch 实际收益 |
| N-5 | 滚动/刷新导致源卡 bounds 失效时 morph 静默降级 fallback | 转场会话 | 体验不一致，非纯性能但相关 |
| N-6 | Nav entry 自定义 ViewModelStoreOwner patch，深层栈弹回多 entry 重建 | BiliPaiNavDisplayHost.kt:742-776 | 深栈返回开销 |

### 6.3 测量方案

现有工具最齐的维度：

1. **宏基准**：`BiliPaiBottomPagerFrameTimingBenchmark`（Tab 切换）+ `BiliPaiVideoDetailFrameTimingBenchmark`（16 个用例，含整卡打开/返回、预测返回完成/取消、8 次往返循环）。
2. **分相位实采**：`release_card_transition_sample.sh` + `video_card_transition_report.py`，直接用其自带门禁（超预算>5、2×超预算>1、PSS 增长>16MB）做判定。
3. **N-1 取舍量化**：临时构建两组（beyondViewportPageCount=0 vs =4），对比冷启动后内存（meminfo）与首次切 Tab 帧耗时——给「常驻 vs 重建」一个数字结论。
4. **N-2 归因**：Compose 编译器指标 + Perfetto track「recompose」区段，识别 AppNavigation 大重组的实际触发源。
5. **返回链路**（N-4）：Perfetto 抓返回 420ms 窗口内的主线程（重挂载 + 封面解码 + 景深模糊同帧竞争），分别量化三项占比。
6. **预测返回**：四种风格各采 10 次，确认风格切换不引入回归。

### 6.4 判定要点

- Tab 切换 p95 < 1.5× 帧预算；转场各相位按 report.py 门禁全绿。
- N-1/N-4 的结论格式：数字对比 + 明确保留/调整建议。

---

## 7. 维度四：液态玻璃（模糊）

### 7.1 代码地图

- 统一入口：`core/ui/blur/UnifiedBlur.kt:83-156`（`Modifier.unifiedBlur`，surfaceType+开关+MotionTier+运行时守卫）
- 预算：`design-system/.../blur/BlurBudgetPolicy.kt:19-85`（Reduced 档→0；**滚动/转场期间 allowRealtime=false**；inputScale 0.82–0.88 降采样）
- 降级三层：`core/ui/blur/RecoverableVisualEffects.kt:38-135`（API 门槛 / RuntimeVisualGuard / 前后台 gate）；守卫策略 `design-system/.../adaptive/RuntimeVisualGuardPolicy.kt`
- 背景源复用：`core/ui/blur/ChromeBackdropSource.kt:39-64`（单次 GraphicsLayer 捕获、多 destination 复放）
- 使用面：40+ 文件（首页/动态顶栏、液态底栏 `FloatingBottomBar.kt`（1265 行）、评论 sheet、直播、BackToTop、侧抽屉等）
- 复用审计文档：`docs/liquid-glass-reuse-audit.md`（结论：透镜/色散链路已复用 Miuix Backdrop 基建；**缺设备帧率回归验证**——本维度补上）

### 7.2 重点疑点

| 编号 | 疑点 | 位置 | 风险 |
|---|---|---|---|
| G-1 | **预算只控单实例等级、不控同屏实例数**：首页同屏可同时存在顶栏 dock + 底栏 dock + BackToTop + 骨架等多个 hazeEffect，每个都是一次 RenderEffect 离屏 pass | BlurBudgetPolicy.kt + 使用面盘点 | 同屏 GPU pass 数无上限（疑似最大风险） |
| G-2 | `unifiedBlur` 的 remember 依赖 isScrolling/isTransitioning，状态翻转瞬间重建 hazeEffect 配置（配置重组 + 采样分辨率切换） | UnifiedBlur.kt:111-125 | 滚动启停瞬间尖峰 |
| G-3 | 常驻 0.82–0.88 降采样层：性能换画质的既有取舍，需确认暗色内容边缘可感知度与真实收益 | BlurBudgetPolicy.kt:73-85 | 画质/性能平衡点未验证 |
| G-4 | 滚动/转场「关实时模糊」策略是否在所有路径都真实生效 | BlurBudgetPolicy + 40+ 调用点 | 个别调用点绕过预算 |
| G-5 | 液态底栏拖拽期间透镜逐帧形变捕获层 | FloatingBottomBar.kt 多处 graphicsLayer+onDrawSurface | 拖拽帧率（文档自认未验证） |

### 7.3 测量方案

1. **同屏实例普查**（G-1）：静态列出每个主场景（首页/动态/播放页/评论 sheet）实际挂载的 blur 实例数与半径；再用 Perfetto GPU 轨道量化每实例成本。产出「场景 × 实例数 × GPU 耗时」表——这是本维度核心交付。
2. **滚动启停尖峰**（G-2）：fling 起/停各 20 次，gfxinfo framestats 看状态翻转帧是否有 >2× 尖峰。
3. **实例数实验**（G-1 判定）：临时逐个禁用实例做 A/B（BackToTop、骨架、dock），得单实例边际成本，决定是否需要「同屏预算上限」策略。
4. **降采样 A/B**（G-3）：inputScale 1.0 / 0.88 / 0.82 三组帧耗时 + 截图肉眼评估。
5. **G-4 审计**：逐个调用点核对 surfaceType 分类与预算路由，跑 `design-system` 既有 blur policy 测试（`BlurIntensityVisualPolicyTest` 模式），补缺失用例。
6. **降级路径**：低端机（API 29/30）确认 blur 完全关闭路径的表现；中端机人为施压验证 RuntimeVisualGuard 触发→降档→恢复全链路时序（60s 冷却是否合适）。
7. **底栏拖拽**（G-5）：`release_home_scroll_sample.sh` 思路改造成拖拽采样，或直接 Perfetto。

### 7.4 判定要点

- 滚动中：所有实时 blur 必须确认已被预算挂起（G-4 全绿）。
- 静止态同屏 GPU blur 总耗时给出数值；据此定「同屏实例预算」是否立为 P1。
- RuntimeVisualGuard 在正常使用中触发频率 <1 次/小时（频繁触发说明常态已超预算）。

---

## 8. 维度五：UI 动效

### 8.1 代码地图

- 统一规格：`design-system/.../motion/AppMotionTokens.kt`（easing/spring/tween token，:18-169）；feature 层另有约 22 个 MotionSpec/Policy（`HomeWaterfallMotionSpec`、`CommonListMotionSpec` 等）
- 骨架屏：`core/ui/skeleton/SkeletonBreathing.kt:24-41`（2.8s 无限呼吸，reduceMotion 定格）+ `ContentLoadingSkeletons.kt` + 用户开关
- 列表动效：`core/util/Animations.kt`（`animateEnter` :91-143 index stagger、`bouncyClickable` :145-172）；进场门控 `feature/home/HomeCardEnterAnimationPolicy.kt:8-31`（滚动中/返回时不播）
- 底栏/透镜：DampedDragAnimation（design-system 与 feature/home 两份实现）

### 8.2 重点疑点

| 编号 | 疑点 | 位置 | 风险 |
|---|---|---|---|
| A-1 | 骨架屏无限循环动画阻止设备 idle，常驻场景（慢网络）下耗电 | SkeletonBreathing.kt | 功耗而非帧率 |
| A-2 | `bouncyClickable` 为每个可点卡片建 interactionSource+Animatable，长列表实例多（读值在 draw 阶段，理论可控） | Animations.kt:145-172 | 实例规模 × 边际成本需实测 |
| A-3 | `animateEnter` 门控只在首次 composition 读一次（刻意取舍），滚动中挂载的卡是否真的不播需要验证 | HomeCardEnterAnimationPolicy.kt | 快速滚动时 spring 并发 |
| A-4 | 22 个分散 MotionSpec 与 AppMotionTokens 的漂移（时长/曲线不一致） | 各 feature | 一致性 + 未来调优成本 |
| A-5 | 双轨动画系统（Compose ContentTransform vs Miuix NavTransition）曾叠加双层转场，现以 None 解耦——新增路由误配是回归点 | BiliPaiNavContentTransformPolicy.kt:30-36 | 回归风险，需测试守护 |

### 8.3 测量方案

1. **A-1 功耗**：骨架屏常驻 10 分钟 vs 无骨架，`dumpsys batterystats` / `dumpsys power` 对比；顺带验证 doze 下的行为。
2. **A-2/A-3**：首页 3 屏快速 fling，JankStats + compose metrics（recomposition 计数），对比「门控开/关」。
3. **A-4**：静态审计表——22 个 MotionSpec 的时长/曲线与 AppMotionTokens 对照，输出漂移清单（纯代码工作，半天）。
4. **A-5 守护**：为「新路由不得同时配置 ContentTransform 与 NavTransition」补一个策略单测（仿 `AppNavigationAppearancePolicyTest` 模式）。
5. **reduceMotion/无障碍**：系统「移除动画」开启下全场景走查（骨架定格、animateEnter 跳过、转场直切）。

### 8.4 判定要点

- 动效本身以「不产生 >2× 帧预算尖峰」为底线；功耗项给数字不定硬阈值。
- A-4 漂移清单作为低成本快赢项，可先行修复。

---

## 9. 跨维度专项

1. **内存与泄漏**（贯穿五维度）：LeakCanary（`-Pbili.debug.leakCanary=true`）跑「播 5 个视频 + 切 5 Tab + 进出小窗」标准剧本；重点核查 MiniPlayerManager 单例（媒体会话/通知/UI 状态缓存一身）与延迟 `player.release()`（`Handler.post`）在快速连续进出时的窗口期。
2. **启动**：Phase 0 已含 4 种 CompilationMode 对比；补「baseline profile 生效前后」A/B 作为 profile 价值的直接证据。
3. **低端机降级路径端到端**：MotionTier.Reduced + blur 全关 + 弹幕降密度，跑完整用户剧本，确认降级后 jank 达标——降级是兜底不是遮羞布。
4. **内存压力场景**：`adb shell am send-trim-memory` 分级触发，验证各级回调行为（后台禁视频轨、idle 释放等策略路径）。

---

## 10. 指标与阈值总表（建议值，Phase 0 基线出来后校准）

| 指标 | 阈值 | 适用 |
|---|---|---|
| 帧超预算数（单场景采集窗口） | ≤5 帧 | 转场各相位（沿用 report.py 门禁） |
| 2× 超预算帧 | ≤1 帧 | 转场各相位 |
| jank 率（>16.7ms 帧占比） | <5%（滚动/弹幕稳态）、<1%（播放稳态，以 DecoderCounters 计） | 各维度 |
| p99 帧耗时 | <2× 帧预算（60Hz 即 <33.4ms） | 除起播外所有 UI 场景 |
| `BiliPaiDanmakuSetData` 主线程耗时 | <8ms | 弹幕 |
| RuntimeVisualGuard 触发频率 | <1 次/小时（正常使用） | 液态玻璃 |
| PSS 增长（单场景采集前后） | ≤16MB | 转场（沿用既有门禁）、播放 30min |
| 起播首帧 | 先测基线，阈值 = 基线中位数 ×1.2 作为回归线 | 视频播放 |

---

## 11. 执行顺序与产出

### 顺序（总计约 7–10 个工作日）

```
Phase 0 基线（0.5-1d）
  → 弹幕（1-1.5d，疑点最集中、现有 benchmark 最成熟）
  → 页面切换与转场（1.5d，工具最齐）
  → 液态玻璃（1.5-2d，需 GPU 归因）
  → 视频播放（2d，链路最长）
  → UI 动效（1d，偏轻）
  → 跨维度收尾 + 报告（1d）
```

弹幕和转场提前是因为：疑点密度最高（D-1、N-1）、现成测量工具最完整，能最快出有信服力的结论；视频播放依赖 Perfetto 脚本（Phase 0 产出）。

### 产出物

1. `docs/perf/baseline-2026Q4.md`：基线数字总表（Phase 0 交付）。
2. `docs/perf/hotspots-2026Q4.md`：热点清单，每条含「现象 → 证据（数据+trace）→ 定级 → 修复建议 → 预估收益」。
3. 新脚本：`scripts/perfetto_collect.sh`。
4. 每个维度的策略测试补充（仿既有 policy 测试模式，纯 Kotlin 可测的判定逻辑优先）。
5. 性能门禁建议：哪些既有脚本/benchmark 进 CI、跑什么频率、卡什么阈值。

### 修复原则（审查后的下一步）

- P0（用户可感知卡顿/黑屏/明显延迟）：当场修，修完用同一命令复测，数字回写基线文档。
- P1/P2：进 backlog，按收益/成本排序。
- 每个修复走最小验证（AGENTS.md 验证阶梯），不跑全量任务。
- 完成一个有意义的切片即 commit + push，不攒大包。

---

## 附：本次静态走查已发现的疑点索引

| 编号 | 一句话 | 维度 | 初判 |
|---|---|---|---|
| V-1 | 每换 bvid 全量重建 ExoPlayer（key 含时间戳） | 播放 | 待验证，可能是刻意设计 |
| V-2 | 播放器创建逻辑三处复制、参数漂移 | 播放 | 一致性问题 |
| V-3 | 诊断关闭时 Listener 仍拼字符串/换对象 | 播放 | 疑似 P1 |
| V-5 | 始终 EXTENSION_RENDERER_MODE_ON | 播放 | 低端机风险 |
| V-6 | key(...) 重建 PlayerView → surface 重挂载 | 播放 | 黑屏痛点根源 |
| D-1 | 弹幕窗口提交主线程三次排序 | 弹幕 | 疑似 P0 |
| D-3 | DrawCachePool 容量 8 | 弹幕 | GC 压力 |
| D-4 | mask 每帧全屏 saveLayer | 弹幕 | GPU 大头 |
| D-6 | 高级弹幕 16ms 轮询 O(n) 扫描 | 弹幕 | 疑似 P1 |
| N-1 | 5 Tab 全常驻组合 | 切换 | 待量化取舍 |
| N-2 | AppNavigation 单 composable 大重组 | 切换 | 疑似 P1 |
| N-3 | 转场三层并行 + 逐帧 BlurEffect | 切换 | 需 GPU 归因 |
| G-1 | blur 同屏实例数无预算 | 玻璃 | 疑似最大风险 |
| G-2 | 滚动状态翻转重建 hazeEffect 配置 | 玻璃 | 尖峰 |
| G-4 | 预算「滚动关实时模糊」覆盖度未审计 | 玻璃 | 审计项 |
| A-1 | 骨架屏无限动画阻止 idle | 动效 | 功耗 |
| A-4 | 22 个 MotionSpec 与 token 漂移 | 动效 | 快赢项 |
