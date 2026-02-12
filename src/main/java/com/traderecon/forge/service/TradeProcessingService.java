package com.traderecon.forge.service;

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

    @Autowired
    public TradeProcessingService(
            List<TradeProcessor> processors,
            @Value("${processing.thread-pool-size:8}") int threadPoolSize,
            @Value("${processing.timeout-seconds:30}") int timeoutSeconds
    ) {
        this.processors = processors;
        this.timeoutSeconds = timeoutSeconds;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        log.info("TradeProcessingService initialized with {} processors and {} threads",
                processors.size(), threadPoolSize);
    }

    /**
     * Process a trade through the complete workflow.
     */
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();

        try {
            // Find appropriate processor
            TradeProcessor processor = findProcessor(trade.getTradeType());

            // Process asynchronously with timeout
            CompletableFuture<ProcessingResult> future = CompletableFuture.supplyAsync(
                    () -> processor.process(trade),
                    executorService
            ).orTimeout(timeoutSeconds, TimeUnit.SECONDS);

            ProcessingResult result = future.join();

            // Update processing time
            long duration = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(duration);

            return result;

        } catch (CompletionException e) {
            log.error("Processing failed for trade: {}", trade.getTradeId(), e.getCause());
            return ProcessingResult.failure(
                    trade.getTradeId(),
                    ProcessingStatus.PROCESSING_FAILED,
                    e.getCause().getMessage()
            );

        } catch (Exception e) {
            log.error("Unexpected error processing trade: {}", trade.getTradeId(), e);
            return ProcessingResult.failure(
                    trade.getTradeId(),
                    ProcessingStatus.PROCESSING_FAILED,
                    e.getMessage()
            );
        }
    }

    /**
     * Find processor that supports the given trade type.
     */
    private TradeProcessor findProcessor(TradeType type) {
        return processors.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No processor found for trade type: " + type));
    }
}