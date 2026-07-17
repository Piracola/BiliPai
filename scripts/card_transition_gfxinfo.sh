#!/usr/bin/env bash
# Debug 版：采集「视频卡片进/出详情」期间的 gfxinfo 掉帧。
# 用法：
#   1) 安装并打开 debug App，停在首页 feed
#   2) 另开终端跑本脚本
#   3) 脚本提示后，连续做 5～8 次：点卡片进详情 → 返回
#   4) 看输出的 janky 比例，并可用 logcat 看 VideoCardTransition hitch
set -euo pipefail

PKG="${PKG:-com.android.purebilibili.debug}"
DEVICE="${1:-}"
ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB=(adb -s "$DEVICE")
fi

OUT_DIR="docs/perf/raw"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$OUT_DIR/card-transition-gfxinfo-${STAMP}.txt"

echo "[card-transition] package=$PKG"
echo "[card-transition] 确保 App 已在前台（首页），然后按 Enter 开始采集…"
read -r _

"${ADB[@]}" shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true
echo "[card-transition] 采集中：请连续 5～8 次 点卡片→返回（约 20～30 秒）"
echo "[card-transition] 做完后按 Enter 结束…"
read -r _

"${ADB[@]}" shell dumpsys gfxinfo "$PKG" > "$OUT"
echo "[card-transition] wrote $OUT"

# 摘要常见字段
python3 - <<PY
from pathlib import Path
text = Path("$OUT").read_text(errors="ignore")
keys = (
    "Total frames rendered",
    "Janky frames",
    "50th percentile",
    "90th percentile",
    "95th percentile",
    "99th percentile",
    "Number Missed Vsync",
    "Number High input latency",
    "Number Slow UI thread",
    "Number Slow bitmap uploads",
    "Number Slow issue draw commands",
)
print("[card-transition] summary:")
for line in text.splitlines():
    if any(k in line for k in keys):
        print(" ", line.strip())
PY

echo
echo "[card-transition] 同步看 live-blur hitch："
echo "  adb logcat -d -s VideoCardTransition:W VideoCardTransition:I | tail -80"
echo "[card-transition] Studio：Profiler → CPU → Frame Timeline / System Trace，过滤过渡时段"
