# Tech Pulse starter backend

Small Spring Boot backend for `www.lin3.de` with a built-in contact form and a base for a future tech insight feed.

## Features

- Public landing page at `/`
- Public tech updates page at `/updates.html`
- Contact API at `POST /api/contact`
- Tech updates API at `GET /api/updates?limit=8`
- Manual refresh API at `POST /api/updates/refresh?limit=8`
- Refresh common limits API at `POST /api/updates/refresh-all`
- Updates health API at `GET /api/health/updates`
- `updates.html` includes a "Refresh now" button and source status badge.
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
- Optional Ollama summarization can be enabled with `techpulse.llm.enabled=true`.
- Auto refresh can be enabled with `techpulse.updates.auto-refresh.enabled=true`.
- Integrations for AI/Kafka/Elasticsearch are disabled by default in `src/main/resources/application.properties`.

