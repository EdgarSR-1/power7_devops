#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JMX_TEMPLATE="$ROOT_DIR/jmeter/login-load.jmx"
RESULT_DIR="$ROOT_DIR/jmeter/load-results"
SUMMARY_FILE="$RESULT_DIR/summary.txt"

HOST="${1:-159.54.144.40}"
CSV_FILE="${CSV:-$ROOT_DIR/jmeter/users.csv}"
DURATION="${DURATION:-120}"
RAMPUP_MIN="${RAMPUP_MIN:-5}"
STEPS=(5 10 20 50 100 200)

if ! command -v jmeter >/dev/null 2>&1; then
  echo "jmeter not found in PATH"
  exit 1
fi

if [ ! -f "$JMX_TEMPLATE" ]; then
  echo "JMX template not found: $JMX_TEMPLATE"
  exit 1
fi

if [ ! -f "$CSV_FILE" ]; then
  echo "CSV file not found: $CSV_FILE"
  exit 1
fi

mkdir -p "$RESULT_DIR"
: > "$SUMMARY_FILE"

echo "Backend host: $HOST"
echo "CSV file: $CSV_FILE"
echo "Duration per step: ${DURATION}s"
echo "Results dir: $RESULT_DIR"
echo

for THREADS in "${STEPS[@]}"; do
  TMP_JMX="$(mktemp /tmp/login-load.XXXXXX.jmx)"
  OUT_FILE="$RESULT_DIR/results-${THREADS}.jtl"
  RAMPUP="$THREADS"
  if [ "$RAMPUP" -lt "$RAMPUP_MIN" ]; then
    RAMPUP="$RAMPUP_MIN"
  fi

  cp "$JMX_TEMPLATE" "$TMP_JMX"
  perl -0pi -e "s|(<intProp name=\"ThreadGroup.num_threads\">)5(</intProp>)|\\1${THREADS}\\2|; s|(<intProp name=\"ThreadGroup.ramp_time\">)10(</intProp>)|\\1${RAMPUP}\\2|; s|(<boolProp name=\"ThreadGroup.scheduler\">)false(</boolProp>)|\\1true\\2|; s|(<boolProp name=\"LoopController.continue_forever\">)false(</boolProp>)|\\1true\\2|; s|(<stringProp name=\"ThreadGroup.duration\">)[^<]*(</stringProp>)|\\1${DURATION}\\2|" "$TMP_JMX"

  echo "=== Running ${THREADS} users for ${DURATION}s ==="
  jmeter -n -t "$TMP_JMX" -l "$OUT_FILE" -JHOST="$HOST" -JCSV="$CSV_FILE"

  awk -v t="$THREADS" -F, '
    NR > 1 {
      count++;
      if ($8 == "false") fail++;
      code[$4]++;
      sum += $2;
      if (min == "" || $2 < min) min = $2;
      if (max == "" || $2 > max) max = $2;
    }
    END {
      if (count == 0) {
        printf("threads=%s samples=0\n", t);
        exit;
      }
      printf("threads=%s samples=%d fail=%d success=%d avg_ms=%.1f min_ms=%s max_ms=%s", t, count, fail, count - fail, sum / count, min, max);
      for (k in code) {
        printf(" %s=%d", k, code[k]);
      }
      print "";
    }
  ' "$OUT_FILE" | tee -a "$SUMMARY_FILE"

  rm -f "$TMP_JMX"
  echo
 done

echo "Summary written to: $SUMMARY_FILE"
