# Real-Time Clickstream Analytics

Real-time streaming analytics pipeline that ingests user click events via REST API, streams through Apache Kafka, and
aggregates metrics into time-windowed buckets stored in PostgreSQL.

## Architecture

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐     ┌────────────┐
│  Client  │────→│  REST API    │────→│  Kafka Topic    │────→│  Consumer    │────→│ PostgreSQL │
│          │     │  (async)     │     │  (3 partitions) │     │  (group)     │     │  (metrics) │
└──────────┘     └──────────────┘     └─────────────────┘     └──────────────┘     └────────────┘
                       │                      │                      │
                  HTTP 202             sessionId as key        Hourly windowed
                  Accepted             Ordered per session     aggregation
```

### Data Flow

1. **Ingestion** — Click events received via `POST /api/events`
2. **Production** — Published to Kafka with `sessionId` as partition key
3. **Streaming** — Distributed across 3 partitions, maintaining per-session ordering
4. **Consumption** — Consumer group reads and delegates to aggregation service
5. **Aggregation** — Timestamps truncated to hourly windows, counters upserted
6. **Query** — Aggregated metrics exposed via `GET /api/metrics/*`

## Tech Stack

| Component      | Technology               | Purpose                        |
|----------------|--------------------------|--------------------------------|
| Application    | Java 17, Spring Boot 4   | REST API, Kafka integration    |
| Message Broker | Apache Kafka 4.1 (KRaft) | Event streaming, partitioning  |
| Database       | PostgreSQL 16            | Aggregated metrics storage     |
| Monitoring     | Kafka UI (Kafbat)        | Topic inspection, consumer lag |
| Infrastructure | Docker Compose           | Local environment setup        |

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

### 3. Send Events

```bash
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
```

### 4. Query Metrics

```bash
# Summary
curl -s http://localhost:8090/api/metrics/summary | jq .

# By page
curl -s http://localhost:8090/api/metrics/page/products | jq .

# Full report (API + PostgreSQL)
./scripts/query-metrics.sh
```

### 5. Load Testing

```bash
# Generate 100 randomized events
./scripts/generate-test-data.sh

# Performance test — 1,000 events with throughput measurement
./scripts/performance-test.sh
```

## API

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

## Design Decisions

### Partition Key — `sessionId`

Events keyed by `sessionId` guarantee all events within a session land in the same partition and are processed in order.
This enables accurate session analytics (duration, funnel tracking) without consumer-side sorting.

**Trade-off:** Potential hot partitions if a single session generates disproportionate traffic. Acceptable for
clickstream workloads where sessions are short-lived and well-distributed.

### Windowed Aggregation — Hourly Buckets

Timestamps truncated to the hour before storage. Multiple events for the same `(pageUrl, eventType, hour)` increment a
single counter rather than creating individual rows. Millions of raw events compress into thousands of metric rows.

### Async Producer — `acks=1`

Asynchronous send with leader-only acknowledgment. Prioritizes throughput over guaranteed delivery — appropriate for
analytics where occasional event loss is acceptable.

### Consumer Group — Auto Rebalancing

Single consumer group with automatic partition assignment. Horizontally scalable — additional instances trigger
automatic rebalancing across partitions.

## Monitoring

- **Kafka UI** — `http://localhost:8080` — Topics, partitions, consumer groups, message inspection
- **Application Logs** — Producer and consumer activity logged at DEBUG level; errors at ERROR
- **PostgreSQL** — Direct SQL queries for metric analysis

## Project Structure

```
clickstream-analytics/
├── docker-compose.yml
├── pom.xml
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

---

*[Eranga Herath](https://github.com/erangaherathofficial)*