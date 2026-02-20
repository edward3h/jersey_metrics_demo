# Jersey Metrics Demo

A self-contained demonstration of instrumenting a Jersey 3 / JAX-RS web service with [Micrometer](https://micrometer.io/) and pushing metrics via OTLP directly to [Grafana Cloud](https://grafana.com/products/cloud/) — no local Prometheus or OpenTelemetry Collector required.

**Stack:** Maven · Jersey 3 (Grizzly embedded) · Micrometer OTLP registry → Grafana Cloud (Mimir)

---

## Prerequisites

- Java 17 (pinned via `.tool-versions` for [asdf](https://asdf-vm.com/))
- Maven 3.x
- A [Grafana Cloud](https://grafana.com/auth/sign-up/create-user) account (free tier is sufficient)

```bash
# If using asdf, install the pinned JDK once after cloning
asdf install
```

---

## Grafana Cloud credentials

You need three values from your Grafana Cloud stack. Navigate to **grafana.com → My Account → your stack → Send metrics** (or the Details link next to Prometheus):

| Environment variable | Where to find it |
|---|---|
| `GRAFANA_CLOUD_INSTANCE_ID` | **Instance ID** (numeric) on the Prometheus/Metrics details page |
| `GRAFANA_CLOUD_OTLP_ENDPOINT` | **OTLP endpoint** URL on the same page |
| `GRAFANA_CLOUD_API_TOKEN` | **grafana.com → Security → API Keys** — create a key with the *MetricsPublisher* role |

```bash
cp .env.example .env
# edit .env and fill in the three values
```

---

## Running locally

```bash
mvn package -DskipTests
source .env
java -jar target/jersey-metrics-demo-1.0-SNAPSHOT.jar
```

## Running with Docker Compose

```bash
docker compose up --build
```

The `.env` file is read automatically by Compose.

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/inventory` | Current stock level |
| `POST` | `/inventory?amount=N` | Add stock |
| `DELETE` | `/inventory?amount=N` | Remove stock |
| `POST` | `/orders` | Create an order (JSON body) — ~10% simulated failure rate, 10–200 ms latency |
| `GET` | `/orders` | List all orders |
| `POST` | `/simulate/burst?count=N` | Fire N concurrent orders (max 10 000) |

```bash
# Basic smoke test
curl http://localhost:8080/inventory

# Generate load
curl -X POST "http://localhost:8080/simulate/burst?count=100"
```

---

## Metrics

The following custom metrics are published in addition to standard JVM and HTTP server metrics:

| Metric | Type | Description |
|---|---|---|
| `orders.created` | Counter | Successfully created orders |
| `orders.failed` | Counter | Orders that hit the simulated failure path |
| `orders.processing.time` | Timer (p50/p95/p99) | Order processing latency |
| `inventory.stock.level` | Gauge | Current stock level |

All HTTP requests are automatically timed as `http.server.requests` by the Jersey Micrometer integration.

Metrics are pushed every 15 seconds. They should appear in Grafana Cloud within ~30 seconds of the app starting.

### Viewing in Grafana Cloud

1. Open your Grafana Cloud stack → **Explore**
2. Select the **Mimir** (or Prometheus) datasource
3. Query `{service_name="jersey-metrics-demo"}` to see all metrics

Example PromQL queries:

```promql
# HTTP request rate
rate(http_server_requests_seconds_count[1m])

# p95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap usage
jvm_memory_used_bytes{area="heap"}

# Order throughput
increase(orders_created_total[1m])

# Inventory level
inventory_stock_level
```

Import community dashboard **4701** ("JVM Micrometer") for a ready-made JVM overview.
