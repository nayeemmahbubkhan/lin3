# Tech Pulse starter backend

Small Spring Boot backend for `www.lin3.de` with a built-in contact form and a base for a future tech insight feed.

## Features

- Public landing page at `/`
- Public tech updates page at `/updates.html`
- Contact API at `POST /api/contact`
- Tech updates API at `GET /api/updates?limit=8`
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
- Optional Ollama summarization can be enabled with `techpulse.llm.enabled=true`.
- Integrations for AI/Kafka/Elasticsearch are disabled by default in `src/main/resources/application.properties`.

