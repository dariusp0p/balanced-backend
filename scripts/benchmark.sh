#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
REQUESTS="${2:-100}"

EMAIL="benchmark.$(date +%s)@example.com"
PASSWORD="password123"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

now_ms() {
  python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
}

SIGNUP_PAYLOAD=$(cat <<JSON
{"name":"Benchmark User","email":"$EMAIL","password":"$PASSWORD","confirmPassword":"$PASSWORD"}
JSON
)

curl -s -X POST "$BASE_URL/auth/signup" \
  -H "Content-Type: application/json" \
  -d "$SIGNUP_PAYLOAD" > /dev/null

LOGIN_PAYLOAD=$(cat <<JSON
{"email":"$EMAIL","password":"$PASSWORD"}
JSON
)

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD")

TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [[ -z "$TOKEN" ]]; then
  echo "Failed to obtain token from login response"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

for i in $(seq 1 20); do
  HOUR=$((7 + (i % 12)))
  MINUTE=$((i % 60))
  printf -v TIME "%02d:%02d" "$HOUR" "$MINUTE"
  PAYLOAD=$(cat <<JSON
{"name":"Seed Meal $i","date":"2024-03-24","time":"$TIME","calories":220,"protein":18,"carbs":28,"fats":4}
JSON
)
  curl -s -X POST "$BASE_URL/api/food-logs" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" > /dev/null
 done

run_case() {
  local name="$1"
  local command_type="$2"
  local endpoint="$3"
  local payload="${4:-}"

  local safe_name
  safe_name=$(echo "$name" | tr '/ ' '__')
  local out_file="$TMP_DIR/$safe_name.txt"
  : > "$out_file"

  local start_ms
  start_ms=$(now_ms)

  for _ in $(seq 1 "$REQUESTS"); do
    if [[ "$command_type" == "POST" ]]; then
      curl -s -o /dev/null -w "%{time_total}\n" -X POST "$BASE_URL$endpoint" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$payload" >> "$out_file"
    else
      curl -s -o /dev/null -w "%{time_total}\n" "$BASE_URL$endpoint" \
        -H "Authorization: Bearer $TOKEN" >> "$out_file"
    fi
  done

  local end_ms
  end_ms=$(now_ms)
  local elapsed_ms=$((end_ms - start_ms))

  awk -v elapsed_ms="$elapsed_ms" -v count="$REQUESTS" -v scenario="$name" '
  {
    times[NR] = $1 * 1000;
    sum += times[NR];
  }
  END {
    n = NR;
    asort(times);
    p50_index = int(n * 0.50);
    if (p50_index < 1) p50_index = 1;
    p95_index = int(n * 0.95);
    if (p95_index < 1) p95_index = 1;
    p99_index = int(n * 0.99);
    if (p99_index < 1) p99_index = 1;

    avg = (n > 0) ? sum / n : 0;
    p50 = (n > 0) ? times[p50_index] : 0;
    p95 = (n > 0) ? times[p95_index] : 0;
    p99 = (n > 0) ? times[p99_index] : 0;
    rps = (elapsed_ms > 0) ? (count * 1000.0) / elapsed_ms : 0;

    printf("| %s | %d | %.2f | %.2f | %.2f | %.2f | %.2f |\n", scenario, count, rps, avg, p50, p95, p99);
  }
  ' "$out_file"
}

echo "| Scenario | Requests | Req/s | Avg ms | P50 ms | P95 ms | P99 ms |"
echo "|---|---:|---:|---:|---:|---:|---:|"

LOGIN_BENCH_PAYLOAD='{"email":"'$EMAIL'","password":"'$PASSWORD'"}'
run_case "POST /auth/login" "POST" "/auth/login" "$LOGIN_BENCH_PAYLOAD"
run_case "GET /api/food-logs?page=0&size=10" "GET" "/api/food-logs?page=0&size=10"
run_case "GET /api/food-logs/stats" "GET" "/api/food-logs/stats"
