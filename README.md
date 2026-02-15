# Real-Time Clickstream Analytics

Production-grade real-time streaming analytics pipeline built with Apache Kafka, Spring Boot, and PostgreSQL. Ingests
user click events via REST API, streams through Kafka, and aggregates metrics into time-windowed buckets for analytics.

## Architecture

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐     ┌────────────┐
│  Client  │────→│  REST API    │────→│  Kafka Topic    │────→│  Consumer    │────→│ PostgreSQL │
│  (curl)  │     │  Controller  │     │  (3 partitions) │     │  (group)     │     │  (metrics) │
└──────────┘     └──────────────┘     └─────────────────┘     └──────────────┘     └────────────┘
                       │                      │                      │
                  HTTP 202             sessionId as key        Hourly windowed
                  Accepted             Ordered per session     aggregation
```

### Data Flow

1. **Ingestion** — Client sends click events via `POST /api/events`
2. **Production** — `ClickEventProducer` publishes to Kafka with `sessionId` as partition key
3. **Streaming** — Kafka distributes events across 3 partitions, maintaining per-session ordering
4. **Consumption** — `ClickEventConsumer` reads events from the consumer group
5. **Aggregation** — `MetricsService` truncates timestamps to hourly windows and upserts counters
6. **Query** — `GET /api/metrics/summary` returns aggregated analytics

## Tech Stack

| Component      | Technology               | Purpose                        |
|----------------|--------------------------|--------------------------------|
| Application    | Java 17, Spring Boot 4   | REST API, Kafka integration    |
| Message Broker | Apache Kafka 4.1 (KRaft) | Event streaming, partitioning  |
| Database       | PostgreSQL 16            | Aggregated metrics storage     |
| Monitoring     | Kafka UI (Kafbat)        | Topic inspection, consumer lag |
| Infrastructure | Docker Compose           | One-command local setup        |

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven (or use included `mvnw`)

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:

- **Kafka** on `localhost:29092` (host) / `kafka:9092` (Docker internal)
- **Kafka UI** on `localhost:8080`
- **PostgreSQL** on `localhost:5432`

### 2. Start the Application

```bash
./mvnw spring-boot:run
```

Application starts on `localhost:8090`.

### 3. Send Test Events

```bash
# Single event
curl -X POST http://localhost:8090/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "sessionId": "sess-abc-123",
    "userId": "user-456",
    "eventType": "PAGE_VIEW",
    "timestamp": "2025-01-15T10:30:45Z",
    "pageUrl": "/products/laptop-x1",
    "elementId": null,
    "ipAddress": "192.168.1.10",
    "device": "mobile",
    "browser": "Chrome 120",
    "location": "US"
  }'

# Generate 100 randomized events
./scripts/generate-test-data.sh

# Performance test (1,000 events)
./scripts/performance-test.sh
```

### 4. Query Metrics

```bash
# Summary
curl -s http://localhost:8090/api/metrics/summary | jq .

# All metrics
curl -s http://localhost:8090/api/metrics | jq .

# By page
curl -s http://localhost:8090/api/metrics/page/products | jq .

# Full report (API + SQL)
./scripts/query-metrics.sh
```

## API Documentation

### Events

| Method | Endpoint      | Description           | Response |
|--------|---------------|-----------------------|----------|
| POST   | `/api/events` | Publish a click event | 202      |

### Metrics

| Method | Endpoint                  | Description                    | Response |
|--------|---------------------------|--------------------------------|----------|
| GET    | `/api/metrics`            | All aggregated metrics         | 200      |
| GET    | `/api/metrics/page/{url}` | Metrics for a specific page    | 200      |
| GET    | `/api/metrics/summary`    | Summary with totals and counts | 200      |

### Health

| Method | Endpoint  | Description    | Response |
|--------|-----------|----------------|----------|
| GET    | `/health` | Service health | 200      |

## Key Design Decisions

### Partition Key Strategy — `sessionId`

Events are keyed by `sessionId`, guaranteeing all events within a session land in the same partition and are processed
in order. This enables accurate session analytics (duration, funnel tracking) without consumer-side sorting.

**Trade-off:** Potential hot partitions if a single session generates disproportionate traffic. Acceptable for
clickstream workloads where sessions are short-lived and well-distributed.

### Windowed Aggregation — Hourly Buckets

Timestamps are truncated to the hour before storage. Multiple events for the same `(pageUrl, eventType, hour)` increment
a single counter rather than creating individual rows.

**Result:** Millions of raw events compress into thousands of metric rows. Fast queries, minimal storage.

### Async Producer with `acks=1`

The producer sends events asynchronously with leader-only acknowledgment. This prioritizes throughput over guaranteed
delivery — appropriate for analytics where losing a few events is acceptable.

### Consumer Group — Single Group, Auto Rebalancing

One consumer group (`clickstream-analytics-group`) with automatic partition assignment. If multiple instances are
deployed, Kafka rebalances partitions across consumers automatically.

## Monitoring

- **Kafka UI**: `http://localhost:8080` — Inspect topics, partitions, consumer groups, and messages
- **Application Logs**: Producer logs partition/offset on send; consumer logs on receive
- **PostgreSQL**: Direct SQL queries for deep metric analysis

## Production Considerations

This is a learning project. For production, you would need:

- **Schema Registry + Avro** — Enforce schema evolution, reduce payload size
- **Idempotent Producer** (`acks=all` + `enable.idempotence=true`) — Prevent duplicate events
- **Dead Letter Topic** — Route failed events instead of losing them
- **Consumer Error Handling** — Retry policies, circuit breakers
- **Database Connection Pooling** — HikariCP tuning for high throughput
- **Horizontal Scaling** — Multiple consumer instances behind a load balancer
- **Security** — SASL_SSL for Kafka, input validation, rate limiting
- **Observability** — Prometheus metrics, Grafana dashboards, distributed tracing
- **Data Retention** — Kafka topic retention policies, PostgreSQL partitioning

## Project Structure

```
clickstream-analytics/
├── docker-compose.yml
├── pom.xml
├── README.md
├── scripts/
│   ├── generate-test-data.sh
│   ├── performance-test.sh
│   └── query-metrics.sh
└── src/main/java/com/erangaherath/clickstream/
    ├── ClickstreamAnalyticsApplication.java
    ├── config/
    │   └── KafkaTopicConfig.java
    ├── controller/
    │   ├── ClickEventController.java
    │   ├── HealthController.java
    │   └── MetricsController.java
    ├── consumer/
    │   └── ClickEventConsumer.java
    ├── model/
    │   ├── ClickEvent.java
    │   ├── EventType.java
    │   └── PageViewMetric.java
    ├── producer/
    │   └── ClickEventProducer.java
    ├── repository/
    │   └── PageViewMetricRepository.java
    └── service/
        └── MetricsService.java
```

## Kafka Concepts Applied

| Concept             | Implementation                                            |
|---------------------|-----------------------------------------------------------|
| Topics & Partitions | `clickstream-events` with 3 partitions                    |
| Message Keys        | `sessionId` for partition-based ordering                  |
| Producer            | Async send, `acks=1`, 3 retries                           |
| Consumer Groups     | `clickstream-analytics-group`, auto offset commit         |
| Offset Management   | `earliest` reset policy, committed offsets on restart     |
| Serialization       | JSON via Spring Kafka `JsonSerializer`/`JsonDeserializer` |
| Topic Configuration | Programmatic creation via `TopicBuilder`                  |

## Built With

Part of a 10-project streaming engineering portfolio. This is **Project 1: Kafka Fundamentals**.

---

*Built by [Eranga Herath](https://github.com/erangaherath)*