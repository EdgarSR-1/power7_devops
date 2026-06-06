#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLAN_FILE="${PLAN_FILE:-$SCRIPT_DIR/reto-load-test.jmx}"
RESULT_DIR="${RESULT_DIR:-$SCRIPT_DIR/results}"
REPORT_DIR="${REPORT_DIR:-$SCRIPT_DIR/reports}"

STAGES="${STAGES:-5 10 25 50}"
DURATION="${DURATION:-120}"
RAMP_UP="${RAMP_UP:-30}"
THINK_TIME_MS="${THINK_TIME_MS:-500}"
THINK_TIME_JITTER_MS="${THINK_TIME_JITTER_MS:-1000}"
P95_LIMIT_MS="${P95_LIMIT_MS:-1500}"
ERROR_LIMIT_PCT="${ERROR_LIMIT_PCT:-1}"

TARGET_PROTOCOL="${TARGET_PROTOCOL:-http}"
TARGET_HOST="${TARGET_HOST:-localhost}"
TARGET_PORT="${TARGET_PORT:-8080}"

if [[ -n "${TARGET_URL:-}" ]]; then
  TARGET_PROTOCOL="${TARGET_URL%%://*}"
  URL_REMAINDER="${TARGET_URL#*://}"
  HOST_PORT="${URL_REMAINDER%%/*}"
  TARGET_HOST="${HOST_PORT%%:*}"
  if [[ "$HOST_PORT" == *:* ]]; then
    TARGET_PORT="${HOST_PORT##*:}"
  elif [[ "$TARGET_PROTOCOL" == "https" ]]; then
    TARGET_PORT="443"
  else
    TARGET_PORT="80"
  fi
fi

if [[ -n "${JMETER_HOME:-}" && -x "$JMETER_HOME/bin/jmeter" ]]; then
  JMETER_BIN="$JMETER_HOME/bin/jmeter"
elif command -v jmeter >/dev/null 2>&1; then
  JMETER_BIN="$(command -v jmeter)"
else
  echo "JMeter was not found. Set JMETER_HOME or add jmeter to PATH." >&2
  exit 1
fi

mkdir -p "$RESULT_DIR" "$REPORT_DIR"

RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
SUMMARY_FILE="$RESULT_DIR/capacity-summary-$RUN_ID.csv"
printf 'users,samples,avg_ms,p95_ms,error_pct,throughput_per_sec,status,jtl\n' > "$SUMMARY_FILE"

best_users=""

for users in $STAGES; do
  stage_run_id="$RUN_ID-$users"
  jtl_file="$RESULT_DIR/${users}u-$stage_run_id.jtl"
  log_file="$RESULT_DIR/${users}u-$stage_run_id.log"

  echo "Running ${users} users for ${DURATION}s against ${TARGET_PROTOCOL}://${TARGET_HOST}:${TARGET_PORT}"

  "$JMETER_BIN" -n \
    -t "$PLAN_FILE" \
    -l "$jtl_file" \
    -j "$log_file" \
    -Jprotocol="$TARGET_PROTOCOL" \
    -Jhost="$TARGET_HOST" \
    -Jport="$TARGET_PORT" \
    -Jusers="$users" \
    -Jseed_users="$users" \
    -Jramp_up="$RAMP_UP" \
    -Jduration="$DURATION" \
    -Jthink_time_ms="$THINK_TIME_MS" \
    -Jthink_time_jitter_ms="$THINK_TIME_JITTER_MS" \
    -Jrun_id="$stage_run_id" \
    -Jjmeter.save.saveservice.output_format=csv \
    -Jjmeter.save.saveservice.print_field_names=true

  samples="$(awk -F, 'NR > 1 { n++ } END { print n + 0 }' "$jtl_file")"
  if [[ "$samples" == "0" ]]; then
    avg_ms="0"
    p95_ms="0"
    error_pct="100"
    throughput="0"
    status="FAIL"
  else
    stats="$(awk -F, 'NR > 1 {
        n++
        sum += $2
        if ($8 != "true") fail++
        if (n == 1 || $1 < min_ts) min_ts = $1
        if (n == 1 || $1 > max_ts) max_ts = $1
      }
      END {
        elapsed_s = (max_ts - min_ts) / 1000
        if (elapsed_s <= 0) elapsed_s = 1
        printf "%.2f,%.2f,%.2f", sum / n, 100 * fail / n, n / elapsed_s
      }' "$jtl_file")"
    avg_ms="${stats%%,*}"
    rest="${stats#*,}"
    error_pct="${rest%%,*}"
    throughput="${rest#*,}"

    p95_index="$(( (samples * 95 + 99) / 100 ))"
    p95_ms="$(awk -F, 'NR > 1 { print $2 }' "$jtl_file" | sort -n | awk -v idx="$p95_index" 'NR == idx { print; found = 1; exit } END { if (!found) print 0 }')"

    status="$(awk -v errors="$error_pct" -v p95="$p95_ms" -v max_errors="$ERROR_LIMIT_PCT" -v max_p95="$P95_LIMIT_MS" 'BEGIN {
        print (errors <= max_errors && p95 <= max_p95) ? "PASS" : "FAIL"
      }')"
  fi

  printf '%s,%s,%s,%s,%s,%s,%s,%s\n' "$users" "$samples" "$avg_ms" "$p95_ms" "$error_pct" "$throughput" "$status" "$jtl_file" >> "$SUMMARY_FILE"

  if [[ "$status" == "PASS" ]]; then
    best_users="$users"
  fi
done

echo
echo "Capacity summary: $SUMMARY_FILE"
column -s, -t "$SUMMARY_FILE" || cat "$SUMMARY_FILE"

if [[ -n "$best_users" ]]; then
  echo
  echo "Recommended concurrent users with current criteria: $best_users"
else
  echo
  echo "No stage passed the current criteria."
fi
