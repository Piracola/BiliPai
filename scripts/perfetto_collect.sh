#!/usr/bin/env bash
# Long-window Perfetto capture for BiliPai macro scenarios.
#
# Fills the gap noted in docs/PERFORMANCE_REVIEW_PLAN.md §2.2: fullTracing only
# covers in-benchmark runs, so steady-state playback / tab switching on a real
# device had no system-level trace. This script records main thread,
# RenderThread, GPU frequency and memory counters around a scripted scenario.
#
# Usage:
#   ./scripts/perfetto_collect.sh [--device SERIAL] [--scenario tabs|play|manual]
#                                 [--duration SEC] [--rounds N] [--tabs N]
#                                 [--buffer-kb KB] [--package PKG] [--out NAME]
#                                 [--profile auto|full|light]
#
# Scenarios:
#   tabs    Auto-drive bottom bar tab round trips (default 5 tabs x 6 rounds).
#   play    Launch app, then open a video MANUALLY within the warmup window;
#           the script keeps recording for the whole duration (default 600s).
#   manual  Just record for --duration seconds while you drive the app.
#
# Profiles (ring buffer retains buffer_kb/rate seconds, so window length picks the profile):
#   full  All atrace categories. ~4.9MB/s, so a 128MB ring keeps only ~26s —
#         use it for short windows carrying sched/gfx/render detail.
#   light No atrace categories except "am". ~0.1MB/s — use it for multi-minute
#         windows (frame timing, GPU/CPU freq, meminfo, app Trace sections).
#   auto  full when --duration <= 30s, light otherwise (default).
#
# Notes:
#   - App-side Trace sections (e.g. BiliPaiDanmakuSetData) reach the trace via
#     trace_marker and are captured for release/dev builds. ART GC trace points
#     need a debuggable build; on release/dev infer GC pressure from
#     meminfo polling + sched gaps instead.
#   - Output lands in docs/perf/raw/ (perfetto trace + gfxinfo + meminfo).
set -euo pipefail

# Git Bash (MSYS2) rewrites /data/... arguments into C:/Program Files/Git/data/... before
# adb.exe sees them, which breaks every device-side path below. Exclude them from conversion.
export MSYS2_ARG_CONV_EXCL="/data;/data/*"

PKG="com.android.purebilibili"
DEVICE=""
SCENARIO="tabs"
DURATION=""
ROUNDS=6
TABS=5
TAP_DELAY_SECONDS="1.2"
WARMUP_SECONDS=8
BUFFER_KB=131072
OUT_NAME=""
PROFILE="auto"

usage() {
  sed -n '2,/^[^#]/p' "$0" | grep -E '^#( |$)' | sed 's/^# \{0,1\}//'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="${2:-}"; shift 2 ;;
    --scenario) SCENARIO="${2:-}"; shift 2 ;;
    --duration) DURATION="${2:-}"; shift 2 ;;
    --rounds) ROUNDS="${2:-}"; shift 2 ;;
    --tabs) TABS="${2:-}"; shift 2 ;;
    --tap-delay) TAP_DELAY_SECONDS="${2:-}"; shift 2 ;;
    --warmup-seconds) WARMUP_SECONDS="${2:-}"; shift 2 ;;
    --buffer-kb) BUFFER_KB="${2:-}"; shift 2 ;;
    --package) PKG="${2:-}"; shift 2 ;;
    --out) OUT_NAME="${2:-}"; shift 2 ;;
    --profile) PROFILE="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

case "$SCENARIO" in
  tabs|play|manual) ;;
  *) echo "Unknown scenario: $SCENARIO (tabs|play|manual)" >&2; exit 1 ;;
esac

case "$PROFILE" in
  auto|full|light) ;;
  *) echo "Unknown profile: $PROFILE (auto|full|light)" >&2; exit 1 ;;
esac

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 1
fi

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')"
fi
if [[ -z "$DEVICE" ]]; then
  echo "No online adb device found." >&2
  exit 1
fi

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

SAFE_TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
RAW_DIR="docs/perf/raw"
mkdir -p "$RAW_DIR"
OUT_NAME="${OUT_NAME:-perfetto-${DEVICE}-${SCENARIO}-${SAFE_TIMESTAMP}}"

if [[ -z "$DURATION" ]]; then
  case "$SCENARIO" in
    tabs)
      # warmup + forward+backward taps (+5s margin for tap round-trips)
      DURATION="$(awk -v w="$WARMUP_SECONDS" -v r="$ROUNDS" -v t="$TABS" -v d="$TAP_DELAY_SECONDS" \
        'BEGIN{printf "%d", w + r * t * 2 * d + 5}')"
      ;;
    play) DURATION=600 ;;
    manual) DURATION=120 ;;
  esac
fi

echo "[perfetto] device=$DEVICE scenario=$SCENARIO duration=${DURATION}s buffer=${BUFFER_KB}KB pkg=$PKG"

# --- profile selection ----------------------------------------------------------
# Measured on Xiaomi 2109119BC (SM7325, 120Hz) with Perfetto v34, app in foreground:
#   full  ≈ 4.9 MB/s — ftrace-bound (gfx 1.65, view 1.57, sched 1.25 MB/s, rest 0.43)
#   light ≈ 0.1 MB/s — no ftrace categories except "am"
# The ring buffer only retains size/rate seconds, so a window longer than that keeps
# just the tail. Long scenarios must therefore drop ftrace detail.
FULL_RATE_KBPS=4900
LIGHT_RATE_KBPS=100
if [[ "$PROFILE" == "auto" ]]; then
  if (( DURATION > 30 )); then PROFILE="light"; else PROFILE="full"; fi
fi
if [[ "$PROFILE" == "full" ]]; then
  PROFILE_RATE_KBPS="$FULL_RATE_KBPS"
else
  PROFILE_RATE_KBPS="$LIGHT_RATE_KBPS"
fi
RETENTION_S=$(( BUFFER_KB / PROFILE_RATE_KBPS ))
if (( RETENTION_S < 1 )); then RETENTION_S=1; fi
echo "[perfetto] profile=$PROFILE (~${PROFILE_RATE_KBPS}KB/s) ring retention≈${RETENTION_S}s"
if (( DURATION > RETENTION_S )); then
  echo "[perfetto] WARNING: ${DURATION}s window > ${RETENTION_S}s retention; only the tail survives." >&2
  echo "[perfetto]          Raise --buffer-kb (RAM-bound) or shorten --duration." >&2
fi

# --- screen size for tab tap coordinates -------------------------------------
SIZE_LINE="$(adb_cmd shell wm size | tr -d '\r' | head -n1)"
SIZE="${SIZE_LINE##*: }"
WIDTH="${SIZE%x*}"
HEIGHT="${SIZE#*x}"
if [[ -z "$WIDTH" || -z "$HEIGHT" || "$WIDTH" == "$SIZE" ]]; then
  WIDTH=1080; HEIGHT=2400
fi

# --- device-side config -------------------------------------------------------
# perfetto only reads configs labelled perfetto_configs_data_file. A file pushed to
# /data/local/tmp carries shell_data_file and fails with errno 13 (Permission denied).
CFG_REMOTE="/data/misc/perfetto-configs/bilipai_perfetto.cfg"
TRACE_REMOTE="/data/misc/perfetto-traces/${OUT_NAME}.pftrace"
DURATION_MS=$(( DURATION * 1000 ))

CFG_LOCAL="$(mktemp -t bilipai_perfetto.XXXXXX)"

if [[ "$PROFILE" == "full" ]]; then
  CFG_FTRACE='data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      atrace_categories: "sched"
      atrace_categories: "freq"
      atrace_categories: "idle"
      atrace_categories: "am"
      atrace_categories: "wm"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_categories: "binder_driver"
      atrace_categories: "binder_lock"
      atrace_categories: "hal"
      atrace_categories: "ss"
      atrace_categories: "database"
    }
  }
}'
else
  # "am" is the only near-free category (~1KB/s); everything else measured above
  # 100KB/s and would blow the ring buffer on a multi-minute window.
  CFG_FTRACE='data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      atrace_categories: "am"
    }
  }
}'
fi

cat > "$CFG_LOCAL" <<EOF
buffers {
  size_kb: ${BUFFER_KB}
  fill_policy: RING_BUFFER
}
duration_ms: ${DURATION_MS}
${CFG_FTRACE}
data_sources {
  config {
    name: "android.surfaceflinger.frametimeline"
  }
}
data_sources {
  config {
    name: "linux.sys_stats"
    sys_stats_config {
      cpufreq_period_ms: 250
      devfreq_period_ms: 250
      meminfo_period_ms: 1000
      vmstat_period_ms: 5000
      stat_period_ms: 5000
    }
  }
}
data_sources {
  config {
    name: "linux.process_stats"
    process_stats_config {
      scan_all_processes_on_start: true
      proc_stats_poll_ms: 1000
    }
  }
}
data_sources {
  config {
    name: "track_event"
  }
}
flush_period_ms: 10000
incremental_state_config {
  clear_period_ms: 10000
}
EOF
# core.autocrlf=true checks this script out with CRLF; a stray \r would land in the
# text-format config, so normalise before pushing.
tr -d '\r' < "$CFG_LOCAL" > "${CFG_LOCAL}.lf" && mv "${CFG_LOCAL}.lf" "$CFG_LOCAL"
adb_cmd shell "mkdir -p /data/misc/perfetto-configs" >/dev/null
adb_cmd push "$CFG_LOCAL" "$CFG_REMOTE" >/dev/null
rm -f "$CFG_LOCAL"

cleanup() {
  adb_cmd shell rm -f "$CFG_REMOTE" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# --- start capture -------------------------------------------------------------
echo "[perfetto] starting capture (${DURATION}s window)..."
adb_cmd shell rm -f "$TRACE_REMOTE" >/dev/null 2>&1 || true
START_TS="$(date +%s)"
# Duration lives in the config (duration_ms); perfetto rejects --time together with -c.
(
  adb_cmd shell "perfetto -c $CFG_REMOTE --txt -o $TRACE_REMOTE"
) &
PERFETTO_PID=$!
sleep 3

if ! kill -0 "$PERFETTO_PID" 2>/dev/null; then
  echo "[perfetto] ERROR: perfetto exited immediately; check config/device support" >&2
  exit 1
fi

# --- scenario drive ------------------------------------------------------------
# drive_scenario runs in a background subshell, so it cannot report status back via a
# variable; the parent probes the process instead.
drive_scenario() {
  adb_cmd shell input keyevent 3 >/dev/null
  adb_cmd shell am start -W -n "${PKG}/com.android.purebilibili.MainActivity" >/dev/null || true
  sleep "$WARMUP_SECONDS"

  case "$SCENARIO" in
    tabs)
      local round i tx
      local bar_y=$((HEIGHT - 60))
      for round in $(seq 1 "$ROUNDS"); do
        # forward: tab 1 -> N
        for i in $(seq 1 "$TABS"); do
          tx=$((WIDTH * (2 * i - 1) / (2 * TABS)))
          adb_cmd shell input tap "$tx" "$bar_y" >/dev/null
          sleep "$TAP_DELAY_SECONDS"
        done
        # backward: tab N-1 -> 1 (round trip)
        for i in $(seq $((TABS - 1)) -1 1); do
          tx=$((WIDTH * (2 * i - 1) / (2 * TABS)))
          adb_cmd shell input tap "$tx" "$bar_y" >/dev/null
          sleep "$TAP_DELAY_SECONDS"
        done
        echo "[perfetto] tab round $round/$ROUNDS done"
      done
      ;;
    play)
      echo "[perfetto] open a video with danmaku ON now; recording until window ends."
      ;;
    manual)
      echo "[perfetto] drive the app manually; recording until window ends."
      ;;
  esac
}

drive_scenario &
DRIVE_PID=$!

# gfxinfo reset happens after launch so the whole scenario is covered.
adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true

wait "$DRIVE_PID" || true
if adb_cmd shell pidof "$PKG" >/dev/null 2>&1; then LAUNCHED="yes"; else LAUNCHED="no"; fi
ELAPSED=$(( $(date +%s) - START_TS ))
REMAINING=$(( DURATION - ELAPSED ))
if (( REMAINING > 0 )); then
  sleep "$REMAINING"
fi

MEM_BEFORE_KB=""
MEM_FILE="$RAW_DIR/${OUT_NAME}-meminfo.txt"
adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_FILE" 2>/dev/null || true
GFX_FILE="$RAW_DIR/${OUT_NAME}-gfxinfo.txt"
adb_cmd shell dumpsys gfxinfo "$PKG" > "$GFX_FILE" 2>/dev/null || true

# --- stop capture & pull --------------------------------------------------------
echo "[perfetto] stopping capture..."
adb_cmd shell "pkill -TERM perfetto" >/dev/null 2>&1 || true
wait "$PERFETTO_PID" 2>/dev/null || true
sleep 3

TRACE_LOCAL="$RAW_DIR/${OUT_NAME}.pftrace"
adb_cmd pull "$TRACE_REMOTE" "$TRACE_LOCAL" >/dev/null
adb_cmd shell rm -f "$TRACE_REMOTE" >/dev/null

TOTAL_PSS_KB="$(sed -nE 's/.*TOTAL PSS:[[:space:]]*([0-9,]+).*/\1/p' "$MEM_FILE" | head -n1 | tr -d ',')"
JANKY_PERCENT="$(sed -nE 's/.*Janky frames:[[:space:]]*[0-9]+[[:space:]]*\(([0-9.]+)%.*/\1/p' "$GFX_FILE" | head -n1)"

echo "[perfetto] app_launched=$LAUNCHED pss_end=${TOTAL_PSS_KB:-N/A}KB gfx_jank=${JANKY_PERCENT:-N/A}%"
echo "[perfetto] trace: $TRACE_LOCAL"
echo "[perfetto] raw:   $GFX_FILE"
echo "[perfetto] raw:   $MEM_FILE"
echo "[perfetto] next:  open ui.perfetto.dev and load the .pftrace;"
echo "[perfetto]        check tracks: main thread, RenderThread (mali/Adreno), gpufreq, mem."
