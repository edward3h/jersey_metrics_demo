# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and run

```bash
# Build fat JAR (skip tests — no tests exist yet)
mvn package -DskipTests

# Run locally (credentials required via env vars)
source .env
java -jar target/jersey-metrics-demo-1.0-SNAPSHOT.jar

# Run via Docker Compose (reads .env automatically)
docker compose up --build
```

Copy `.env.example` to `.env` and fill in the three Grafana Cloud variables before running.

## Architecture

Single-module Maven project. Entry point is `demo.Main`, which starts an embedded Grizzly HTTP server on port 8080 using a Jersey `ResourceConfig`.

Key wiring in `demo.config.AppConfig` (extends `ResourceConfig`):
- Creates the `OtlpMeterRegistry` via `MetricsFactory.create()`
- Registers `MetricsApplicationEventListener` — this auto-instruments every HTTP request as `http.server.requests` timers
- Binds `MeterRegistry` into HK2 so it can be `@Inject`ed into resources
- Registers the three resource classes

`demo.metrics.MetricsFactory` builds the OTLP push registry:
- Auth is HTTP Basic using `GRAFANA_CLOUD_INSTANCE_ID:GRAFANA_CLOUD_API_TOKEN`
- Endpoint normalisation: appends `/v1/metrics` to `GRAFANA_CLOUD_OTLP_ENDPOINT` if absent
- Uses `AggregationTemporality.DELTA` and a 15 s push step
- Binds standard JVM/system binders on startup

All three resource classes must be `@Singleton` — they hold state (`orders` list, `stockLevel` gauge) and/or inject other singleton resources.

## Key gotchas

- **`ServicesResourceTransformer`** in `maven-shade-plugin` is mandatory — merges Jersey SPI files in the fat JAR; omitting it causes `MessageBodyWriter not found` at runtime.
- **Jakarta namespace** — this is Jersey 3; all imports are `jakarta.*`, not `javax.*`.
- **Java version** — pinned to Temurin 17 via `.tool-versions` (asdf). Run `asdf install` once after cloning.
