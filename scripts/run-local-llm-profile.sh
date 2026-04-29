#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Run Tech Pulse with a local Ollama profile.

Usage:
  ./scripts/run-local-llm-profile.sh <safe|balanced|heavy> [options]

Options:
  --model <name>           Override model for selected profile
  --base-url <url>         Ollama base URL (default: http://localhost:11434)
  --timeout-ms <ms>        Override LLM wait timeout
  --max-concurrency <n>    Override max concurrency
  --dry-run                Print command only (do not start app)
  -h, --help               Show this help

Profiles:
  safe      summary=true, action=false, did-you-know=false, model=qwen2.5:0.5b, timeout=10000, concurrency=2
  balanced  summary=true, action=true, did-you-know=false, model=gemma3:1b, timeout=12000, concurrency=2
  heavy     summary=true, action=true, did-you-know=true, model=gemma2:2b, timeout=20000, concurrency=1
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" || $# -eq 0 ]]; then
  usage
  exit 0
fi

profile="$1"
shift

base_url="http://localhost:11434"
model=""
timeout_ms=""
max_concurrency=""
dry_run="false"

case "$profile" in
  safe)
    model="qwen2.5:0.5b"
    summary_enabled="true"
    action_enabled="false"
    did_you_know_enabled="false"
    timeout_ms="10000"
    max_concurrency="2"
    ;;
  balanced)
    model="gemma3:1b"
    summary_enabled="true"
    action_enabled="true"
    did_you_know_enabled="false"
    timeout_ms="12000"
    max_concurrency="2"
    ;;
  heavy)
    model="gemma2:2b"
    summary_enabled="true"
    action_enabled="true"
    did_you_know_enabled="true"
    timeout_ms="20000"
    max_concurrency="1"
    ;;
  *)
    echo "Unknown profile: $profile" >&2
    usage
    exit 1
    ;;
esac

while [[ $# -gt 0 ]]; do
  case "$1" in
    --model)
      model="${2:-}"
      shift 2
      ;;
    --base-url)
      base_url="${2:-}"
      shift 2
      ;;
    --timeout-ms)
      timeout_ms="${2:-}"
      shift 2
      ;;
    --max-concurrency)
      max_concurrency="${2:-}"
      shift 2
      ;;
    --dry-run)
      dry_run="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$model" ]]; then
  echo "Model must not be empty." >&2
  exit 1
fi

spring_args="--techpulse.llm.enabled=true"
spring_args+=" --techpulse.llm.summary.enabled=${summary_enabled}"
spring_args+=" --techpulse.llm.action.enabled=${action_enabled}"
spring_args+=" --techpulse.llm.did-you-know.enabled=${did_you_know_enabled}"
spring_args+=" --techpulse.llm.base-url=${base_url}"
spring_args+=" --techpulse.llm.model=${model}"
spring_args+=" --techpulse.llm.wait-timeout-ms=${timeout_ms}"
spring_args+=" --techpulse.llm.max-concurrency=${max_concurrency}"

cmd=(./mvnw spring-boot:run -Dspring-boot.run.arguments="$spring_args")

echo "Profile: $profile"
echo "Model:   $model"
echo "Args:    $spring_args"

if [[ "$dry_run" == "true" ]]; then
  echo
  printf 'Dry run command: '
  printf '%q ' "${cmd[@]}"
  echo
  exit 0
fi

exec "${cmd[@]}"

