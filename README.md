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
- Optional PostgreSQL persistence via `postgres` profile

## Run locally

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080`.

Tech feed endpoint example:

```bash
curl "http://localhost:8080/api/updates?limit=5"
curl -X POST "http://localhost:8080/api/updates/refresh?limit=5"
curl -X POST "http://localhost:8080/api/updates/refresh-all"
curl -X POST "http://localhost:8080/api/updates/refresh-all?limits=3,10"
curl "http://localhost:8080/api/health/updates"
curl "http://localhost:8080/api/health/meta"
```

## Run with PostgreSQL

Set database credentials (example values shown), then start with the `postgres` profile:

```bash
export TECHPULSE_DB_URL=jdbc:postgresql://localhost:5432/tech_pulse
export TECHPULSE_DB_USER=techpulse
export TECHPULSE_DB_PASSWORD=techpulse
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Test

```bash
./mvnw test
```

## Notes

- Default mode keeps messages in memory. Use `postgres` profile for persistence.
- Legacy env vars `LIN3_DB_URL`, `LIN3_DB_USER`, and `LIN3_DB_PASSWORD` are still accepted as fallback.
- `GET /api/updates` uses a short in-memory cache; `POST /api/updates/refresh` forces fresh regeneration.
- Updates responses include cache metadata fields: `fromCache` and `cachedAt`.
- Homepage reads `GET /api/health/meta` to show whether local LLM mode is enabled.
- Optional Ollama summarization can be enabled with `techpulse.llm.enabled=true`.
- Auto refresh can be enabled with `techpulse.updates.auto-refresh.enabled=true`.
- Feed quality pipeline (dedup + relevance ranking + stale filtering) is enabled by default.
- GitHub releases source URL is configurable via `techpulse.updates.github-rss-url`.
- Multi-repo GitHub releases ingestion can be configured via `techpulse.updates.github-rss-urls` (comma-separated Atom feed URLs).
- Ranking source weights are configurable via `techpulse.updates.source-weights` (for example `hacker-news:1.0,github-releases:1.2`).
- Integrations for AI/Kafka/Elasticsearch are disabled by default in `src/main/resources/application.properties`.

