# BiliPai 性能热点清单 2026Q4

> 依据 `docs/PERFORMANCE_REVIEW_PLAN.md` 五维度审查产出。编写日期：2026-10-01。
> 证据标记：【实测】= 真机测量数据；【静态】= 代码走查 + file:line（待测量确认）。
> 定级：P0 = 用户可感知卡顿/黑屏/明显延迟，当场修；P1/P2 = 进 backlog。

---

## P0

### H-1 卡片转场在中端机 120Hz 下实际帧率仅 ≈7–10fps 【实测】

- **现象**：首页卡片 → 播放详情的 OPENING morph（291ms）只渲染约 3 帧；返回同理。120Hz 帧预算 8.24ms，实测 p50 超预算 18×（OPENING 147ms / RETURNING 99ms），100% 帧超 2× 预算。即使按 60Hz 预算仍超 6–9×。
- **证据**：`docs/perf/raw/video-card-8f63b62e-20261001-000950.{json,md}`（10 轮往返、相位日志逐轮验证）；基线表 `docs/perf/baseline-2026Q4.md` §3.1。
- **定级**：P0（每次进/出视频详情都可感知的动画顿挫；与用户体感「转场掉帧」一致）。
- **修复方向**（按 N-3 静态结论排序归因）：① 冻结景深层 `VideoCardTransitionBackgroundPolicy` 的逐帧 GPU 全屏模糊在 120Hz 下的占比（Perfetto GPU 轨道，工具 `scripts/perfetto_collect.sh` 就绪）；② 转场期间降分辨率/降采样渲染（对齐 BlurBudgetPolicy 的 inputScale 思路）；③ 评估转场期间强制 60Hz 呈现（blink 时长 <300ms，感知差异小、预算翻倍）。
- **预估收益**：转场帧耗时 ÷2 以上（60Hz 化直接翻倍预算）；若模糊层为主要占比，量化后收益更大。
- **注意**：PSS 门禁在本会话语境下超阈属预期（含播放器启动），不并入本项。

---

## P1

### H-2 弹幕窗口提交在主线程做三次全量排序 + 全量对象映射 【静态，P0 候选待实测】

- **现象**：`replaceWindow` → 首排序（`ByteDanceDanmakuEngine.kt:103`）→ `buildEngineTimeline` 二次排序+全量 map（:296-298）→ `DataManager.setData`（`@UiThread`）三次排序（`DataManager.kt:50-54`）。入参是**过滤合并后的全部弹幕**（`DanmakuManager.kt:436` `cachedDanmakuList`），非窗口子集。调用点全部在主线程（`:349/:1879/:2110`、seek 回调 `:1543/:2266`）。
- **证据**：静态（上引）；同文件 :107 已有 `BiliPaiDanmakuSetData` Trace 区段，与 `BiliPaiDanmakuFrameTimingBenchmark` 对接即可实测。静态估算 6 万条跨窗 seek 主线程阻塞 ≈50–120ms（低端 150–250ms），远超计划阈值 8ms。
- **修复建议**：仓库层排序一次，`setData` 增加免排序快路径（上游已保证有序）；映射改增量窗口。
- **预估收益**：跨窗 seek 主线程阻塞从 ~100ms 量级降到 <8ms；消除连发 seek 卡顿。

### H-3 单条弹幕插入触发 5 次全量排序 【静态】

- **现象**：发一条弹幕 = `DanmakuManager.kt:2390-2391` 两次全量 `plus().sortedBy()` + `:2397 resyncDanmakuTimeline` 再走 H-2 路径 3 次。
- **修复建议**：归并插入（insert by binary search）；与 H-2 一并修。

### H-4 每换 bvid/每次进详情全量重建 ExoPlayer 【静态】

- **现象**：`VideoPlayerState.kt:994` `remember { System.currentTimeMillis() }` 作 playerCreationKey + `:999` remember(bvid, key) → 每次进详情必然重建 RenderersFactory/LoadControl/ExoPlayer 本体。`:992-993` 注释自证是修「重复打开同一视频无声音」的权宜之计。OkHttpClient 与 SimpleCache 是单例，不在重建范围（计划原文该点证伪）。
- **证据**：静态；重建成本实测待 Perfetto（起播链路）。
- **修复建议**：修复释放/持有生命周期后移除时间戳 key；评估 player 复用池（同 §4.4 判定要点：先测重建成本再决定）。
- **预估收益**：起播延迟 -数十 ms 级 + 内存抖动消除。

### H-5 播放器创建参数三处（实为 5+ 处）复制漂移 【静态】

- **现象**：三处创建点差异表（VideoPlayerState / MiniPlayerManager:1805 / PortraitVideoPager:648）：竖屏滑动页**无媒体缓存工厂、无 LoadControl 限流、无 handleAudioBecomingNoisy/wakeMode**；decoderFallback 仅详情页软解分支有。`setShowBuffering` 五处三种配置（NEVER/WHEN_PLAYING/ALWAYS）。另 `BasePlayerViewModel.kt:254/:328`、`BangumiPlayerScreen.kt:178`、`VideoPlaybackUseCase.kt:1227/:1255` 还各自手搓。
- **修复建议**：收敛单一 `PlayerFactory`（core/player），参数集中管理；行为差异显式化。

### H-6 液态玻璃同屏实例数无预算：直播弹幕气泡是极值场景 【静态】

- **现象**：BlurBudgetPolicy 仅单实例视角（G-1 确认）。竖屏直播间**每条可见消息气泡独立挂裸 `hazeEffectCompat`**（`LivePortraitChrome.kt:374`，items @:184），同屏约 10–22 个实时 blur 实例，完全不接预算；首页液态玻璃极值 ≈14 个实例（底栏 3 drawBackdrop + 顶栏 2 + 卡面 N + BackToTop）。
- **证据**：静态普查表见审查记录（场景 × 实例 × 半径）；GPU 成本待 Perfetto 量化。
- **修复建议**：直播气泡改「单 capture 层 + 逐条低级特效」或直接降级为半透明底色（气泡小、blur 视觉收益低）；建立同屏实例预算（P1 立项依据）。
- **预估收益**：直播间 GPU pass 数量级下降（10–22 → 1–3）。

### H-7 骨架屏开关逻辑疑似写反 + fallback 分支丢失 reduceMotion 【静态，行为 bug】

- **现象**：`HomeFeedSkeletonCard.kt:50-67` 与 `ContentLoadingSkeletons.kt:52-70`：用户关掉「呼吸」开关后**反而落入另一条无限动画**（tween(2000) / shimmer tween(1000)），且该分支不检查系统 reduceMotion。慢网络下骨架常驻 = 动画永不停止、设备无法 idle（A-1 确认）。
- **修复建议**：开关语义反转修正；所有无限动画统一接 `rememberSystemReduceMotion()`。
- **预估收益**：功耗（常驻场景 CPU/GPU 占用归零）；顺带修 A-2 的双 bouncyClickable 实现漂移。

### H-8 `animateEnter` 滚动门控从未接线（恒 false） 【静态，接线 bug】

- **现象**：`HomeCardEnterAnimationPolicy` 的 `isScrollInProgress` 形参在所有调用点传入的是 `scrollLiteModeEnabled` 且硬编码 false（`VideoCard.kt:508/:630/:701`、`HomeCategoryPage.kt:436/:479` 等）——「快速滚动时不播入场动画」这一腿在产线上从不生效，快滑中每张新挂载卡并发跑 spring（A-3 确认）。
- **修复建议**：把真实滚动态传入（或按既有注释改成整体关闭快滑动画）。
- **预估收益**：快速 fling 时去除每卡 spring 并发；与滚动基线 p99（34ms）联合看可压尖峰。

### H-9 AppNavigation 根作用域约 20 个被观察状态源，含高频热源 【静态】

- **现象**：N-2 确认。`downloadTasks`（下载进度流）与 audio playlist/active（`:1487-1489`）在播放/下载期间高频更新 → **整个 AppNavigation 根重组**（含 NavDisplay 宿主、HorizontalPager、底栏）。
- **修复建议**：高频 state 下放到消费点；`distinctUntilChanged`/节流；低频设置类可后置。
- **预估收益**：消除下载/播放期间周期性全局重组。

### H-10 beyondViewportPageCount=4 的取舍未量化且对冲不完整 【静态】

- **现象**：N-1 确认。首帧后 5 Tab 全常驻（含各自 blur chrome 与图片列表组合树）；切页低模糊对冲只接了底栏与 Profile，`forceLowBlurBudget` 字段未接到任何页面内容。
- **修复建议**：凭据修复后跑「beyondViewportPageCount=0 vs 4」内存/首切帧耗时 A/B（计划 §6.3-3），用数字决定保留或改 1–2 页。
- **预估收益**：内存 -数十 MB 量级（若收敛）或确认现状合理（保留依据入档）。

### H-11 mask（智能防遮挡）开启即每帧全屏 saveLayer + 无条件 invalidate 【静态】

- **现象**：D-4 确认。`RenderEngine.kt:123-137` saveLayer 条件仅为 `config.mask.enable`（与是否存在活动 mask 帧无关），且 `DanmakuController.kt:252` mask 开启时静止画面也逐帧重绘。1080×2400 每帧约 10MB 离屏分配。
- **修复建议**：仅存在活动 MaskData 帧才 saveLayer；静止时恢复 pauseInvalidate。
- **预估收益**：mask 开启时 GPU 大头消除（中低端机差异放大，待 A/B 实测数值）。

---

## P2（择要）

| 编号 | 内容 | 证据 | 修复建议 |
|---|---|---|---|
| H-12 | D-2：弹幕每帧 5 层聚合重排 + 3 处 LinkedList 重建（ArrayList 优化未贯彻） | RenderEngine.kt:116-121、ScrollLayer.kt:47 | 绘制集合仅进出屏时变化，可缓存排序结果 |
| H-13 | D-3：DrawCachePool 容量 8 在常态密度下池失效（屏上并发 30–80）| DrawCachePool.kt:43-62 | 容量提至 ~64/开放数组池；与 H-3 叠加放大 |
| H-14 | D-6：高级弹幕 16ms 轮询 + remember key 每 tick 失效导致 derivedStateOf 缓存失效、每 tick 全量 filter | AdvancedDanmakuOverlay.kt:62-100 | 用 withFrameNanos/动画时钟替代轮询；filter 结果按列表版本缓存 |
| H-15 | G-2：滚动启停瞬间所有接信号实例的 inputScale 翻转重建 hazeEffect 配置（None↔Fixed(0.82-0.88)） | UnifiedBlur.kt:111-155 | 固定 inputScale（消翻转）或对齐到两档常量避免参数重算 |
| H-16 | G-4：约 15 组裸 blur 路径绕过预算；「allowRealtime=false」实为降采样而非关模糊（G-3 部分证伪：0.82-0.88 仅滚动/转场/降档期生效，非常驻） | 审查记录清单 | 逐点核对路由到统一入口；MusicPlayerContent 逐行歌词/评论线程返回覆盖模糊优先（逐帧重建） |
| H-17 | V-3/V-7：播放器回调与 loadVideo 入口在诊断关闭时仍拼字符串 + 整对象替换 debugInfo StateFlow；`Logger.w` 全局无门禁 | VideoPlayerState.kt:589/:647/:686/:817、Logger.kt:361-376 | 惰性 lambda + 诊断开关前置；w 级别接入开关策略 |
| H-18 | N-3 例外：`videoCardTransitionLiveBackgroundEffect` 每帧 new BlurEffect（同文件 overlay 版本已有缓存） | VideoCardTransitionBackgroundPolicy.kt:768-777 | 对齐 :312-352 的缓存写法 |
| H-19 | N-4：封面 prefetch 注册表 `onCardVisible` 每次重建整表 O(n)/卡；预测手势首帧未预热（注释与实现不符） | HomeCoverReturnPrefetch.kt:36-46 | 增量维护注册表；补手势首帧触发 |
| H-20 | 每可见卡每帧 `layer.record{}`（未冻结时） | VideoCardNativeSnapshot.kt:92-99 | 按卡内容变化节流录制 |
| H-21 | V-5 反向偏差：硬解分支缺 `setEnableDecoderFallback`（健壮性非性能）；「软解误选」证伪（MODE_ON 顺序保证硬解优先） | VideoPlayerState.kt:1041-1050 | 硬解分支补 fallback |
| H-22 | D-7：漂移同步每 tick 无条件 Log.d（秒级频率，量小） | DanmakuManager.kt:1349-1352 | 改 `Logger.d{}` 惰性门控 |
| H-23 | 直播弹幕气泡外的裸 hazeEffectCompat 点（横屏聊天 overlay、发送条等） | LivePortraitChrome.kt:151/:704、LivePlayerScreen.kt:1431 | 路由到统一入口（与 H-16 合并审计） |

### 快赢项（半天级）

1. **A-4 MotionSpec 漂移**：33 个文件全量对照完成。关键漂移：双入场动画系统（app `Animations.kt` Normal=0.70/350 vs ds `AppEntranceMotion` 自称真相源 0.90/380，另有第三套 stagger 52ms^1.38）；`HomeRefreshMotionSpec` 硬编码 5 组 spring 而 `pullRefreshReleaseSpring` token 存在且被测试锁定却零引用；`CommonListMotionSpec` stiffness 260 vs spatialSpec 380（-32%）；骨架脉冲 4 套时钟（2800/2000/2000/1000ms）。孤儿参数清单与逐文件表见审查记录。护栏缺口：`HardcodedMotionLintTest` 有白名单豁免、无任何测试强制 MotionSpec 消费 token——建议先补「token 消费率」测试再批量收敛。
2. **H-7/H-8**（行为 bug 级，改动小、收益直接）。
3. **H-18**（对齐既有缓存写法，几行改动）。

---

## A-5 守护测试（已写入，运行待凭据恢复）

新增 `app/src/test/.../navigation3/BiliPaiNavDoubleTransitionExclusivityTest.kt`，锁定双轨互斥不变量：
1. morph/受管路由（NO_OP_SHARED_ELEMENT + 4×CARD_DISABLED_*）在 Compose ContentTransform 轨必须解析为 None/None；
2. `resolveBiliPaiNavContentTransform` 在 main 源码保持**零调用**（当前 Compose 轨完全休眠，实际转场全走 Miuix 轨——这是现状防回归的关键事实）；
3. 新增路由必须在策略中显式声明 Compose 轨解析，禁止隐式落入 FALLBACK。
验证命令（凭据修复后）：`./gradlew :app:testDebugUnitTest --tests 'com.android.purebilibili.navigation3.BiliPaiNavDoubleTransitionExclusivityTest'`

---

## 性能门禁建议（哪些进 CI、频率、阈值）

| 工具 | 频率 | 门禁阈值 | 说明 |
|---|---|---|---|
| `video_card_transition_report.py`（转场分相位） | 每晚 + 触碰 `core/ui/transition/**`、`navigation3/**` 的 PR | 超预算>5、2×>1、PSS≤16MB（沿用 report.py 自带门禁）；转场相位 p50 需另立基线（当前实测远超预算，先修后门禁） | 现成脚本，纯 adb，任何真机可跑 |
| `mobile_perf_collect.sh` | 每晚 | jank<5%、p99<2× 帧预算 | 本次中端 2.67%/p99 34ms 为首条基线 |
| `BiliPaiDanmakuFrameTimingBenchmark` | 触碰 `danmaku-engine/**`、`feature/video/danmaku/**` 的 PR | `BiliPaiDanmakuSetData` 主线程 <8ms；稳态 jank<5% | macrobenchmark，凭据修复后接入 |
| `BlurIntensityVisualPolicyTest` 等 policy 测试 | 每 PR（已有） | 保持绿色 | design-system 既有 |
| 新增：A-5 双轨互斥测试 | 每 PR | 保持绿色 | 防双层转场回归 |
| `RuntimeVisualGuardTracker` 生产遥测 | 线上 | 触发 <1 次/小时 | 计划 §7.4 阈值 |
| 暂不进 CI：`release_smoke_gate.sh`（改动面大时手动）、Perfetto 长场景（人工归因用） | — | — | — |

---

## 本轮证据不足 / 未覆盖（如实记录）

- 弹幕各疑点（H-2/H-11/H-13/H-14）仅有静态估算，待 macrobenchmark + Perfetto 实测（工具与 Trace 埋点已就绪）。
- 低端真机降级路径（MotionTier.Reduced、blur 全关、RuntimeVisualGuard 触发频率）——无低端设备。
- 播放稳态 30min 内存曲线、起播全链路分阶段计时——需 Perfetto 长采集与真机手动场景，脚本已就绪未执行。
- 液态玻璃同屏实例的 GPU 成本（H-6/H-16）——需 GPU 轨道归因。
