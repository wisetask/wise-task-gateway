#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${K6_IMAGE:-grafana/k6:latest}"

ENV_ARGS=()
pass_env() {
  if [[ -n "${!1:-}" ]]; then
    ENV_ARGS+=("-e" "$1=${!1}")
  fi
}

pass_env GATEWAY_URL
pass_env LOADTEST_LABEL
pass_env LOADTEST_OUT
pass_env LOADTEST_SIGN_IN_EMAIL
pass_env LOADTEST_SIGN_IN_PASSWORD
pass_env LOADTEST_SOLVE_TASK_ID

K6_ARGS=()
map_arg() {
  local env_name="$1"
  local value="$2"
  ENV_ARGS+=("-e" "$env_name=$value")
}

OUT_SET=0
while [[ $# -gt 0 ]]; do
  arg="$1"
  value=""
  if [[ "$arg" == --*=* ]]; then
    value="${arg#*=}"
    arg="${arg%%=*}"
    shift
  elif [[ $# -ge 2 && "${2:-}" != --* ]]; then
    value="${2:-}"
    shift 2
  else
    shift
  fi
  case "$arg" in
    --url) map_arg GATEWAY_URL "$value" ;;
    --vus) map_arg LOADTEST_VUS "$value" ;;
    --duration) map_arg LOADTEST_DURATION "$value" ;;
    --ramp-up) map_arg LOADTEST_RAMP_UP "$value" ;;
    --ramp-down) map_arg LOADTEST_RAMP_DOWN "$value" ;;
    --no-ramp) map_arg LOADTEST_NO_RAMP "1" ;;
    --timeout) map_arg LOADTEST_TIMEOUT "$value" ;;
    --think-time) map_arg LOADTEST_THINK_TIME "$value" ;;
    --think-time-jitter) map_arg LOADTEST_THINK_TIME_JITTER "$value" ;;
    --email) map_arg LOADTEST_SIGN_IN_EMAIL "$value" ;;
    --password) map_arg LOADTEST_SIGN_IN_PASSWORD "$value" ;;
    --solve-task-id) map_arg LOADTEST_SOLVE_TASK_ID "$value" ;;
    --sign-up) map_arg LOADTEST_SIGN_UP "${value:-1}" ;;
    --writes) map_arg LOADTEST_WRITES "${value:-1}" ;;
    --graph-vertices) map_arg LOADTEST_GRAPH_VERTICES "$value" ;;
    --graph-edges) map_arg LOADTEST_GRAPH_EDGES "$value" ;;
    --label) map_arg LOADTEST_LABEL "$value"; LABEL_VALUE="$value" ;;
    --out) map_arg LOADTEST_OUT "$value"; OUT_SET=1 ;;
    *) K6_ARGS+=("$arg${value:+=$value}") ;;
  esac
done

STAMP="$(date +%Y%m%d-%H%M%S)"
LABEL_SUFFIX=""
if [[ -n "${LOADTEST_LABEL:-}" ]]; then
  LABEL_SUFFIX="-$(echo "$LOADTEST_LABEL" | tr -c 'A-Za-z0-9._-' '_')"
elif [[ -n "${LABEL_VALUE:-}" ]]; then
  LABEL_SUFFIX="-$(echo "$LABEL_VALUE" | tr -c 'A-Za-z0-9._-' '_')"
fi
DEFAULT_OUT="results/loadtest-${STAMP}${LABEL_SUFFIX}.json"
if [[ "$OUT_SET" -eq 0 ]]; then
  ENV_ARGS+=("-e" "LOADTEST_OUT=${DEFAULT_OUT}")
fi

mkdir -p "$DIR/results"

exec docker run --rm \
  --network host \
  -u "$(id -u):$(id -g)" \
  -v "$DIR":/loadtests \
  -w /loadtests \
  "${ENV_ARGS[@]}" \
  "$IMAGE" \
  run load-test.js "${K6_ARGS[@]}"
