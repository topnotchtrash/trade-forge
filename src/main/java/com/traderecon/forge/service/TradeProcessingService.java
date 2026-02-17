package com.traderecon.forge.service;

import com.traderecon.forge.metrics.ProcessingMetrics;
import io.annapurna.model.Trade;
import io.annapurna.model.TradeType;
import com.traderecon.forge.model.ProcessingResult;
import com.traderecon.forge.model.ProcessingStatus;
import com.traderecon.forge.processor.TradeProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * Service that orchestrates trade processing workflow.
 *
 * Routes trades to appropriate processors and manages async execution.
 */
@Service
@Slf4j
public class TradeProcessingService {

    private final List<TradeProcessor> processors;
    private final ExecutorService executorService;
    private final int timeoutSeconds;
    private final ProcessingMetrics metrics;

    @Autowired
    public TradeProcessingService(
            List<TradeProcessor> processors,
            @Value("${processing.thread-pool-size:8}") int threadPoolSize,
            @Value("${processing.timeout-seconds:30}") int timeoutSeconds,
            ProcessingMetrics metrics
    ) {
        this.processors = processors;
        this.timeoutSeconds = timeoutSeconds;
        this.metrics = metrics;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();

        // Track active processing
        metrics.incrementActiveProcessing();
        metrics.recordKafkaMessageConsumed();

        try {
            TradeProcessor processor = findProcessor(trade.getTradeType());

            CompletableFuture<ProcessingResult> future = CompletableFuture.supplyAsync(
                    () -> processor.process(trade),
                    executorService
            ).orTimeout(timeoutSeconds, TimeUnit.SECONDS);

            ProcessingResult result = future.join();

            long duration = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(duration);

            // Record metrics
            metrics.recordProcessing(trade.getTradeType(), result.getStatus(), duration);

            // Record validation failures separately
            if (result.getStatus() == ProcessingStatus.VALIDATION_FAILED) {
                metrics.recordValidationFailure(trade.getTradeType());
            }

            return result;

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            long duration = System.currentTimeMillis() - startTime;

            if (cause instanceof TimeoutException) {
                log.error("Processing timeout for trade: {}", trade.getTradeId());
                metrics.recordTimeout(trade.getTradeType());
                return ProcessingResult.timeout(trade.getTradeId());
            }

            log.error("Processing failed for trade: {}", trade.getTradeId(), cause);
            metrics.recordProcessing(trade.getTradeType(), ProcessingStatus.PROCESSING_FAILED, duration);
            return ProcessingResult.failure(
                    trade.getTradeId(),
                    ProcessingStatus.PROCESSING_FAILED,
                    cause.getMessage()
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error processing trade: {}", trade.getTradeId(), e);
            metrics.recordProcessing(trade.getTradeType(), ProcessingStatus.PROCESSING_FAILED, duration);
            return ProcessingResult.failure(
                    trade.getTradeId(),
                    ProcessingStatus.PROCESSING_FAILED,
                    e.getMessage()
            );

        } finally {
            // Always decrement active count
            metrics.decrementActiveProcessing();
        }
    }

    private TradeProcessor findProcessor(TradeType type) {
        return processors.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No processor found for trade type: " + type));
    }
}