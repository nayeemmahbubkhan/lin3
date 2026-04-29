# Tech Pulse starter backend

Small Spring Boot backend for `www.lin3.de` with a homepage dashboard, a simple contact page, and a tech insight feed.

## Features

- Public landing page at `/`
- Public contact page at `/contact.html`
- Contact API at `POST /api/contact`
- Tech updates API at `GET /api/updates?limit=8`
- Manual refresh API at `POST /api/updates/refresh?limit=8`
- Refresh common limits API at `POST /api/updates/refresh-all`
- Updates health API at `GET /api/health/updates`
- Metadata API at `GET /api/health/meta`
- Homepage (`/`) includes refresh controls, latest updates, and source health status.
- Feed ingests multiple sources (Hacker News + GitHub Releases RSS).
- Basic spam trap (hidden `website` field)
- In-memory message storage by default (no external DB required)

## Run locally

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080`.

## Run local LLM (Ollama via Docker)

Quick start:

```bash
docker ps --format '{{.Names}}' | grep -qx 'ollama' || docker run -d --name ollama -p 11434:11434 ollama/ollama
docker exec ollama ollama pull qwen2.5:0.5b
./scripts/run-local-llm-profile.sh safe
```

Profile helper:

```bash
./scripts/run-local-llm-profile.sh safe
./scripts/run-local-llm-profile.sh balanced
./scripts/run-local-llm-profile.sh heavy
./scripts/run-local-llm-profile.sh balanced --model qwen2.5:0.5b
./scripts/run-local-llm-profile.sh safe --dry-run
```

Validate:

```bash
curl -s http://localhost:8080/api/health/meta
curl -s -X POST "http://localhost:8080/api/updates/refresh-all"
curl -s "http://localhost:8080/api/updates?limit=5"
```

Model choice (why Qwen was chosen by default in constrained environments):

- `qwen2.5:0.5b` is significantly lighter and more stable on low-memory hosts.
- `gemma` variants can produce good quality, but `1b/2b` models generally need more RAM and can cause swap/latency spikes on small instances.
- If you want to test Gemma locally, follow the complete guide below and keep per-field toggles selective.

Full guide:

- `docs/local-ollama-gemma-qwen.md`

## Live deployment

Cloud URL: `https://www.lin3.de`

Server-side monitoring (no Google Analytics):

- Request hits are counted from Nginx access logs in CloudWatch (default: `/techpulse/prod/nginx/access`).
- Dashboard shows `raw`, `non-healthcheck`, and `likely-user` hit metrics to avoid Route53 health-check noise.
- Daily/weekly dashboards and alarms are provisioned by `infra/cloudformation/techpulse-single-ec2.yml`.
- Alerts are sent by SNS email after subscription confirmation.

### Cost guard

- Monthly AWS Budget: `techpulse-monthly-guard` (limit: `35 USD`).
- Alerts configured at `80% actual`, `100% actual`, and `100% forecast`.
- Notifications are sent to SNS topic `arn:aws:sns:eu-central-1:649298294157:techpulse-prod-alerts`.

Quick monthly spend snapshot:

```bash
./scripts/aws-monthly-costs.sh
```

Create/update budget from CLI (same setup used in this repo):

```bash
aws budgets describe-budget --account-id 649298294157 --budget-name techpulse-monthly-guard
aws budgets describe-notifications-for-budget --account-id 649298294157 --budget-name techpulse-monthly-guard
```

Tech feed endpoint example:

```bash
curl "http://localhost:8080/api/updates?limit=5"
curl -X POST "http://localhost:8080/api/updates/refresh?limit=5"
curl -X POST "http://localhost:8080/api/updates/refresh-all"
curl -X POST "http://localhost:8080/api/updates/refresh-all?limits=3,10"
curl "http://localhost:8080/api/health/updates"
curl "http://localhost:8080/api/health/meta"
```

## Test

```bash
./mvnw test
```

## Notes

- Contact messages are kept in memory for this MVP runtime.
- `GET /api/updates` uses a short in-memory cache; `POST /api/updates/refresh` forces fresh regeneration.
- Updates responses include cache metadata fields: `fromCache` and `cachedAt`.
- Homepage reads `GET /api/health/meta` to show whether local LLM mode is enabled.
- Optional Ollama summarization can be enabled with `techpulse.llm.enabled=true`.
- Auto refresh can be enabled with `techpulse.updates.auto-refresh.enabled=true`.
- Feed quality pipeline (dedup + relevance ranking + stale filtering) is enabled by default.
- GitHub releases source URL is configurable via `techpulse.updates.github-rss-url`.
- Multi-repo GitHub releases ingestion can be configured via `techpulse.updates.github-rss-urls` (comma-separated Atom feed URLs).
- Ranking source weights are configurable via `techpulse.updates.source-weights` (for example `hacker-news:1.0,github-releases:1.2`).
- Optional AI integrations are disabled by default in `src/main/resources/application.properties`.
