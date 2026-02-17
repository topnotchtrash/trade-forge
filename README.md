# Trade-Forge

A high-throughput Kafka consumer service built with Spring Boot that processes synthetic financial trades in parallel, validates business logic, and simulates production database booking with transaction rollback.

Part of the trade-reconciliation-system project.

---

## Overview

Trade-Forge is a worker service that sits downstream of Trade-Publisher. It consumes trade messages from a Kafka topic, routes them to type-specific processors, validates and enriches each trade, executes a simulated database booking inside a transaction, and rolls back the transaction (dry-run mode). Processing metrics are exposed via Prometheus.

The service is designed to scale horizontally. Multiple instances run as part of the same Kafka consumer group, with Kafka automatically distributing partitions across instances for parallel processing.

---

## Architecture
```
Trade-Publisher → Kafka (trade-recon-input) → Trade-Forge (3 instances)
                                                    ↓
                                             Validate → Enrich → Business Logic → DB (rollback)
                                                    ↓
                                             Prometheus Metrics
```

---

## Trade Types Supported

- Interest Rate Swap
- Equity Swap
- FX Forward
- Equity Option
- Credit Default Swap

---

## Technical Stack

- Java 17
- Spring Boot 3.2.5
- Apache Kafka (Spring Kafka)
- PostgreSQL (Spring Data JPA)
- Micrometer / Prometheus
- Lombok
- Docker

---

## Processing Flow

1. Consume trade message from Kafka topic `trade-recon-input`
2. Deserialize JSON to polymorphic Trade object
3. Set trade type via concrete class inspection
4. Route to appropriate processor based on trade type
5. Validate trade fields and business rules
6. Enrich with mock market data (rates, prices, spreads)
7. Apply business logic and calculate pricing metrics
8. Execute INSERT inside a database transaction
9. Roll back transaction (simulation mode)
10. Commit Kafka offset
11. Emit metrics to Prometheus

---

## Project Structure
```
src/main/java/com/traderecon/forge/
├── TradeForgeApplication.java
├── consumer/
│   └── TradeConsumer.java
├── processor/
│   ├── TradeProcessor.java
│   ├── SwapProcessor.java
│   ├── EquitySwapProcessor.java
│   ├── FXForwardProcessor.java
│   ├── OptionProcessor.java
│   └── CDSProcessor.java
├── service/
│   ├── TradeProcessingService.java
│   ├── ValidationService.java
│   ├── EnrichmentService.java
│   ├── DatabaseService.java
│   └── TradeMapper.java
├── model/
│   ├── TradeRecord.java
│   ├── ProcessingResult.java
│   └── ProcessingStatus.java
├── repository/
│   └── TradeRepository.java
├── config/
│   └── KafkaConsumerConfig.java
├── metrics/
│   └── ProcessingMetrics.java
└── exception/
    ├── ValidationException.java
    └── ProcessingException.java
```

---

## Configuration

Key configuration properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092` | Kafka broker address |
| `DATABASE_URL` | `localhost:5432/trade_recon` | PostgreSQL connection URL |
| `processing.simulation-mode` | `true` | Rollback all DB writes |
| `processing.thread-pool-size` | `8` | Async processing threads |
| `processing.timeout-seconds` | `30` | Per-trade processing timeout |

---

## Running Locally

### Prerequisites

Trade-Publisher must be running first as it provides Kafka, PostgreSQL and Redis:
```bash
cd trade-publisher
docker-compose up -d
```

### Run Trade-Forge on host machine
```bash
cd trade-forge
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Run 3 instances via Docker
```bash
cd trade-forge
docker-compose up --build -d
```

This starts three Trade-Forge instances on ports 8090, 8091 and 8092. Kafka automatically distributes the 10 topic partitions across all instances.

---

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/prometheus` | Prometheus metrics scrape endpoint |

---

## Metrics

The following metrics are exposed at `/actuator/prometheus`:

| Metric | Type | Description |
|--------|------|-------------|
| `trades_processed_total` | Counter | Total trades processed, tagged by type and status |
| `trade_processing_duration_seconds` | Timer | Processing latency per trade type |
| `trades_validation_failed_total` | Counter | Total validation failures by trade type |
| `trades_timeout_total` | Counter | Total processing timeouts |
| `kafka_messages_consumed_total` | Counter | Total Kafka messages consumed |
| `trade_active_processing_count` | Gauge | Trades currently being processed |

---

## Simulation Mode

All database writes are executed inside a transaction that is immediately rolled back. This allows the service to simulate production booking logic including JPA persistence, SQL generation and transaction management without persisting any data.

To disable simulation mode and commit trades to the database, set:
```yaml
processing:
  simulation-mode: false
```

---
## Simulation Notice

The validation and enrichment logic in this service is intentionally simplified for simulation purposes. The `ValidationService` performs structural and business rule checks on incoming trades, while the `EnrichmentService` uses hardcoded market data such as fixed SOFR rates, static equity prices, and formula-based counterparty spreads. In a production environment, these services would be replaced with real implementations: `ValidationService` would integrate with internal reference data systems and counterparty registries, while `EnrichmentService` would call live market data providers such as Bloomberg or Reuters for real-time rates and pricing. The processor classes for each trade type contain simplified pricing formulas that approximate real calculations. These can be extended to include full pricing models such as Black-Scholes for options or discounted cash flow for swaps. All service classes are designed with this extensibility in mind and can be modified independently without changing the core processing pipeline.

---
## Scaling

The service is designed for horizontal scaling via Kafka consumer groups. The topic `trade-recon-input` has 10 partitions. Each instance runs 3 listener threads, giving a total of 9 concurrent consumers across 3 instances.

On AWS this can be extended using:
- EC2 Auto Scaling Groups triggered by CloudWatch alarms on Kafka consumer lag
- KEDA on EKS for Kubernetes-native autoscaling based on lag per partition

---

## Related Services

- **Annapurna** - Synthetic trade data generator library (Maven Central)
- **Trade-Publisher** - Upstream orchestrator that generates and publishes trades to Kafka
- **Sentinel** - Downstream Python reconciliation service