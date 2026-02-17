package com.traderecon.forge.metrics;

import com.traderecon.forge.model.ProcessingStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.annapurna.model.TradeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus metrics for trade processing.
 *
 * Exposes metrics at /actuator/prometheus for Prometheus scraping.
 */
@Component
@Slf4j
public class ProcessingMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeProcessingCount = new AtomicInteger(0);

    public ProcessingMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Register active processing gauge
        Gauge.builder("trade_active_processing_count", activeProcessingCount, AtomicInteger::get)
                .description("Number of trades currently being processed")
                .register(registry);

        log.info("ProcessingMetrics initialized");
    }

    /**
     * Record a completed trade processing.
     */
    public void recordProcessing(TradeType tradeType, ProcessingStatus status, long durationMs) {
        // Increment processed counter with type + status tags
        Counter.builder("trades_processed_total")
                .description("Total trades processed")
                .tag("type", tradeType != null ? tradeType.toString() : "UNKNOWN")
                .tag("status", status.toString())
                .register(registry)
                .increment();

        // Record processing duration
        Timer.builder("trade_processing_duration")
                .description("Trade processing duration in ms")
                .tag("type", tradeType != null ? tradeType.toString() : "UNKNOWN")
                .register(registry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        log.debug("Recorded metrics - Type: {}, Status: {}, Duration: {}ms",
                tradeType, status, durationMs);
    }

    /**
     * Record a validation failure.
     */
    public void recordValidationFailure(TradeType tradeType) {
        Counter.builder("trades_validation_failed_total")
                .description("Total validation failures")
                .tag("type", tradeType != null ? tradeType.toString() : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /**
     * Record a processing timeout.
     */
    public void recordTimeout(TradeType tradeType) {
        Counter.builder("trades_timeout_total")
                .description("Total processing timeouts")
                .tag("type", tradeType != null ? tradeType.toString() : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /**
     * Record a Kafka message consumed.
     */
    public void recordKafkaMessageConsumed() {
        Counter.builder("kafka_messages_consumed_total")
                .description("Total Kafka messages consumed")
                .register(registry)
                .increment();
    }

    /**
     * Increment active processing count.
     */
    public void incrementActiveProcessing() {
        activeProcessingCount.incrementAndGet();
    }

    /**
     * Decrement active processing count.
     */
    public void decrementActiveProcessing() {
        activeProcessingCount.decrementAndGet();
    }
}