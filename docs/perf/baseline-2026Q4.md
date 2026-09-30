# BiliPai 性能基线 2026Q4

> 依据 `docs/PERFORMANCE_REVIEW_PLAN.md` Phase 0 产出。编写日期：2026-10-01。
> 所有优化以此为对照；复测必须使用同表末尾的固化命令与同档位设备。

---

## 1. 设备矩阵（本次实际可用）

| 档位 | 设备 | SoC | 系统 | 备注 |
|---|---|---|---|---|
| 中端真机（主决策设备） | Xiaomi 2109119BC（`8f63b62e`） | SM7325（骁龙 778G） | Android 14（API 34） | 120Hz 实测 vsync ≈121.4Hz |
| 平板/高端参照 | Lenovo TB710FU（`HA2AYBXP`） | SM8650Q（骁龙 8 Gen 3） | Android 16（API 36） | `tablet_perf_collect.sh` 支持设备 |
| 低端真机 | **缺失** | — | — | 降级路径项（0.4、低端 blur/弹幕）本轮未覆盖 |
| 模拟器 pixel6Api31 | 未运行 | — | — | macrobenchmark 参照本轮未覆盖（见 §4 阻塞） |

测量包：`com.android.purebilibili` 0.2.3-alpha.10（release 变体，R8 开启）。遵守 AGENTS.md 打包政策，未使用 debug/smooth 做性能结论。

---

## 2. Phase 0 基础设施结论

### 2.1 Baseline Profile（计划 0.1，最高优先项）—— 结论已修正

- `app/src/main/baseline-prof.txt` **不存在**（`update_baseline_profile.sh` 的第 3 步拷贝从未执行/产物未提交）。
- 但设备实测显示 **merged baseline profile 已随 APK 安装并生效**：
  - 小米中端机：`dumpsys package` → `[status=speed-profile] [reason=baseline]`，`base.odex 9950Kb / base.art 1352Kb`。
  - 平板：`[status=verify] [reason=install]`——刚安装尚未被后台 dexopt 处理（Android 16 的 bg-dexopt 需充电+idle），非 profile 缺失。
- 解释：AGP 自动合并了依赖库 AAR 携带的 baseline profile（Compose/media3 等），**app 自身代码的热路径不在 profile 内**。计划原文「链路闭环但产物未提交，优化可能未实际生效」的担忧部分成立：库代码已覆盖，自有代码未覆盖。
- **结论**：不是 P0 修复项；但「提交 app 自有 hot path 的 baseline-prof.txt」列为 P2（预期收益：自有代码启动/首帧路径 AOT 编译）。

### 2.2 本轮阻塞项（非产品问题，环境凭据）

`settings.gradle.kts` 的 GitHub Packages（miuix SNAPSHOT）凭据失效：`~/.gradle/gradle.properties` 中 `gpr.user/gpr.key` 返回 401（已用 curl 直接验证），gh CLI token 无 `read:packages` scope，本机 Gradle 缓存无 miuix 产物。因此 **所有 `:app` Gradle 任务被阻塞**：

- 计划 0.2（8 个 macrobenchmark，`:baselineprofile:connectedReleaseAndroidTest`）——未执行。
- 计划 0.3（Compose 编译器指标）——未执行（在线/离线均失败）。
- 计划 0.4（RuntimeVisualGuard 降档日志）——logcat 近 2000 行无触发记录，但无法区分「未触发」与「未使用」，本轮不给结论。
- A-5 新增单测——已写入，运行待凭据修复。

**恢复方法**：更新 `~/.gradle/gradle.properties` 的 `gpr.user`/`gpr.key`（需 `read:packages` 权限的 classic PAT），然后跑 `./gradlew :app:compileReleaseKotlin -Pbili.compose.metrics=true` 与 `:baselineprofile:connectedReleaseAndroidTest`。

**替代补偿**：本轮以纯 adb 实采（gfxinfo/meminfo/分相位 checkpoint）补齐了两个维度的真机基线，见 §3。

### 2.3 新增工具

- `scripts/perfetto_collect.sh`：长周期 Perfetto 采集（tabs/play/manual 三场景，抓主线程/RenderThread/GPU 频率/meminfo，数据落 `docs/perf/raw/`）。补齐计划 §2.2 缺口 2。
- Compose 编译器指标开关已确认接线正确（`app/build.gradle.kts:349`），凭据修复后即可产出。

---

## 3. 实测基线数字

### 3.1 卡片转场分相位（中端真机，120Hz，帧预算 8.24ms）

场景：首页 feed 点击左上卡片封面 → 播放详情 → 返回，10 轮往返；`release_card_transition_sample.sh` + `video_card_transition_report.py`，诊断相位日志逐轮验证（10/10 OPENING、10/10 RETURNING）。

| 相位 | 帧数 | p50 | p90 | p95 | p99 | >预算 | >2×预算 | 换算帧率 |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| OPENING（291ms 时长） | 30 | **147.02ms** | 208.20ms | 217.12ms | 217.34ms | 100% | 100% | **≈3 帧/转场，≈7–10fps** |
| RETURNING（242ms 时长） | 34 | **99.31ms** | 114.61ms | 117.98ms | 124.77ms | 100% | 100% | ≈3.4 帧/转场 |

- 门禁判定：**FAIL**（超预算 100% > 5%；2× 超预算 100% > 1%；p90 37.18ms > 8.24ms 目标）。PSS Δ33.66MiB > 16MiB 门禁——已知语境差异：本会话含播放器启动内存，门禁按纯转场会话设计，PSS 项按「仅转场」语义复测时再判。
- 原始数据：`docs/perf/raw/video-card-8f63b62e-20261001-000950.*`（v3 有效样本）；`…-000802.*`（v2，checkpoint 时机过早，仅作对照）；`…-000329.*`（v1 无效：feed 位移导致打开的是个人空间页，无 morph）。
- 与静态疑点 N-3 互证：转场三层并行 + 冻结层 GPU 模糊在中端机 120Hz 下无法达到预算。**即使按 60Hz 预算（16.7ms）p50 仍超 5.9×/8.8×。**
- 待归因（需 Perfetto，工具已就绪）：景深模糊层 vs 飞卡快照 vs NavDisplay transition 三者的帧内占比。

### 3.2 首页滚动（gfxinfo，脚本固定手势）

| 设备 | 帧数 | jank（>deadline） | p50 | p90 | p95 | p99 | PSS 末值 | 判定（<5%） |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| 中端 SD778G @120Hz | 1575 | **2.67%**（42 帧） | 14ms | 19ms | 23ms | 34ms | 446MB | ✅ 达标 |
| 平板 SD8Gen3 | 1729 | **2.08%**（36 帧） | 9ms | 17ms | 23ms | 42ms | 424MB | ✅ 达标 |

- 冷启动耗时（平板脚本附带）：1029ms（report：`docs/perf/6.0.0-tablet-benchmark.md`）。
- 原始数据：`docs/perf/raw/mobile-8f63b62e-20261001-001134-*`、`docs/perf/raw/tablet-HA2AYBXP-20261001-001204-*`。
- 结论：**滚动稳态健康**，首页滚动不是当前瓶颈；p50 14ms（中端 120Hz）说明日常滚动贴近 1× 预算余量不大，但 p99 34ms 无 >3× 尖峰。

### 3.3 待补基线（凭据修复后）

| 项 | 命令 | 状态 |
|---|---|---|
| 弹幕 FrameTiming（6000/6 万条、mask A/B、2x 倍速） | `:baselineprofile:connectedReleaseAndroidTest`（BiliPaiDanmakuFrameTimingBenchmark） | 阻塞 |
| 启动 4 种 CompilationMode | 同上 | 阻塞 |
| 视频详情 16 用例 | 同上 | 阻塞 |
| 非 skippable composable 排行 | `:app:compileReleaseKotlin -Pbili.compose.metrics=true` | 阻塞 |
| 起播全链路 / 播放稳态 30min / 低端降档 | `scripts/perfetto_collect.sh --scenario play`（真机手动开场） | 工具就绪，待执行 |

---

## 4. 复测命令固化

```bash
# 转场分相位（中端机；进入首页后执行；checkpoint 距点击 0.40s/距返回 0.35s，与诊断时长 291/242ms 匹配）
./scripts/release_card_transition_sample.sh start --device <SERIAL> --label <label>
# 每轮：input tap <卡面> → sleep 0.40 → checkpoint；input keyevent 4 → sleep 0.35 → checkpoint
./scripts/release_card_transition_sample.sh stop --device <SERIAL>

# 滚动基线
./scripts/mobile_perf_collect.sh --device 8f63b62e --warmup-seconds 8 --loops 20
./scripts/tablet_perf_collect.sh --device HA2AYBXP

# 长场景系统级 trace（播放/切 Tab/手动）
./scripts/perfetto_collect.sh --device <SERIAL> --scenario tabs|play|manual --duration <s>

# macrobenchmark（凭据修复后）
./gradlew :baselineprofile:connectedReleaseAndroidTest
./gradlew :app:compileReleaseKotlin -Pbili.compose.metrics=true
```

采样注意事项（本轮踩过的坑）：
1. feed 内容会在两次截图之间刷新位移，**每轮必须用 `VideoCardMotion` 相位日志验证 morph 真实发生**（`phase=OPENING/RETURNING` 计数 +1）。
2. 120Hz 下 gfxinfo ring ≈120 帧 ≈1s；checkpoint 必须紧跟转场落定（<0.5s），否则相位帧被冲刷（v2 教训）。
3. Git Bash 环境的 `python3` 是 Microsoft Store stub（rc=49），需 shim 到真实 Python 后再跑报告脚本。
