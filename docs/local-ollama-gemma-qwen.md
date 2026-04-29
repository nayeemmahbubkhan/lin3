# Local Ollama Guide: Gemma and Qwen for Tech Pulse

This guide shows how to run Tech Pulse with local Ollama models on a laptop/dev machine.

## 1) Prerequisites

- Docker installed and running
- Java 21+
- Project root: `/home/nayeem/Projekte/legalmind-ai/backend`

## 2) Start Ollama

```bash
cd /home/nayeem/Projekte/legalmind-ai/backend
docker ps --format '{{.Names}}' | grep -qx 'ollama' || docker run -d --name ollama -p 11434:11434 ollama/ollama
```

Check Ollama health:

```bash
curl -s http://localhost:11434/api/tags
```

## 3) Model matrix (recommended order)

- Low RAM / most stable: `qwen2.5:0.5b`
- Medium: `gemma3:1b` (if available)
- Higher RAM: `gemma2:2b`
- Heavy: `gemma4:latest` (often too heavy for small hosts)

Pull one or more models:

```bash
docker exec ollama ollama pull qwen2.5:0.5b
docker exec ollama ollama pull gemma3:1b
docker exec ollama ollama pull gemma2:2b
```

List installed models:

```bash
docker exec ollama ollama list
```

## 4) Quick model sanity check

Test each model with a short prompt:

```bash
curl -s http://localhost:11434/api/generate -d '{"model":"qwen2.5:0.5b","prompt":"Reply with exactly: ollama-ok","stream":false}'
curl -s http://localhost:11434/api/generate -d '{"model":"gemma3:1b","prompt":"Reply with exactly: ollama-ok","stream":false}'
```

If a model fails or is very slow, use a smaller model.

## 5) Run Tech Pulse with LLM enabled

Use the profile helper script (recommended):

```bash
./scripts/run-local-llm-profile.sh safe
./scripts/run-local-llm-profile.sh balanced
./scripts/run-local-llm-profile.sh heavy
```

Useful overrides:

```bash
./scripts/run-local-llm-profile.sh balanced --model qwen2.5:0.5b
./scripts/run-local-llm-profile.sh safe --timeout-ms 12000 --max-concurrency 1
./scripts/run-local-llm-profile.sh safe --dry-run
```

### Profile definitions

- `safe`: summary only, `qwen2.5:0.5b`, timeout `10000`, concurrency `2`
- `balanced`: summary+action, `gemma3:1b`, timeout `12000`, concurrency `2`
- `heavy`: summary+action+did-you-know, `gemma2:2b`, timeout `20000`, concurrency `1`

### Advanced/manual flags (optional)

If you prefer explicit JVM args instead of the helper script:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--techpulse.llm.enabled=true --techpulse.llm.summary.enabled=true --techpulse.llm.action.enabled=false --techpulse.llm.did-you-know.enabled=false --techpulse.llm.base-url=http://localhost:11434 --techpulse.llm.model=qwen2.5:0.5b --techpulse.llm.wait-timeout-ms=10000 --techpulse.llm.max-concurrency=2"
```

## 6) Validate app behavior

```bash
curl -s http://localhost:8080/api/health/meta
curl -s -X POST "http://localhost:8080/api/updates/refresh-all"
curl -s "http://localhost:8080/api/updates?limit=5"
```

Look for:
- `localLlmEnabled: true` in `/api/health/meta`
- low or no `llmPending` in updates responses
- no long stretches of fallback waiting text

## 7) Troubleshooting

### Model not found

Error example: `pull model manifest: file does not exist`

- Try a different available tag (for example `gemma2:2b` or `gemma3:1b`).
- Check model availability with:

```bash
docker exec ollama ollama list
```

### Slow responses / pending placeholders

- Reduce concurrency to `1` or `2`
- Increase wait timeout
- Disable some fields (`action` / `did-you-know`)
- Switch to `qwen2.5:0.5b`

### Memory pressure

If your system has low memory, prefer:
- model: `qwen2.5:0.5b`
- `--techpulse.llm.max-concurrency=1`
- only `summary` enabled

## 8) Suggested defaults by machine size

- <= 8 GB RAM laptop: `qwen2.5:0.5b`
- 8-16 GB RAM laptop: `qwen2.5:0.5b` or `gemma3:1b`
- > 16 GB RAM: can test `gemma2:2b` and above

For tiny cloud instances, keep fallback-first and enable LLM fields selectively.
