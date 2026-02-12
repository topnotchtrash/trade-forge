package com.traderecon.forge.processor;

import com.traderecon.forge.service.DatabaseService;
import com.traderecon.forge.service.TradeMapper;
import io.annapurna.model.InterestRateSwap;
import io.annapurna.model.Trade;
import io.annapurna.model.TradeType;
import com.traderecon.forge.exception.ValidationException;
import com.traderecon.forge.model.ProcessingResult;
import com.traderecon.forge.model.ProcessingStatus;
import com.traderecon.forge.service.EnrichmentService;
import com.traderecon.forge.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Processor for Interest Rate Swap trades.
 */
@Component
@Slf4j
public class SwapProcessor implements TradeProcessor {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;

    private final DatabaseService databaseService;

    private final TradeMapper tradeMapper;

    @Autowired
    public SwapProcessor(ValidationService validationService, EnrichmentService enrichmentService, TradeMapper tradeMapper, DatabaseService databaseService ) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.tradeMapper = tradeMapper;
        this.databaseService = databaseService;
    }

    @Override
    public boolean supports(TradeType type) {
        return type == TradeType.INTEREST_RATE_SWAP;
    }

    @Override
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();
        InterestRateSwap swap = (InterestRateSwap) trade;

        try {
            // Step 1: Validate
            log.debug("Validating Interest Rate Swap: {}", swap.getTradeId());
            validationService.validateInterestRateSwap(swap);

            // Step 2: Enrich
            log.debug("Enriching Interest Rate Swap: {}", swap.getTradeId());
            BigDecimal floatingRate = enrichmentService.getRateByIndex(swap.getFloatingRateIndex());
            BigDecimal spread = enrichmentService.getSpread(swap.getCounterparty(), swap.getNotional());

            // Step 3: Business Logic (simplified pricing)
            log.debug("Calculating swap pricing for: {}", swap.getTradeId());
            BigDecimal fixedLegPV = calculateFixedLegPV(swap);
            BigDecimal floatingLegPV = calculateFloatingLegPV(swap, floatingRate);
            BigDecimal swapValue = fixedLegPV.subtract(floatingLegPV);

            log.info("Processed IRS {}: FixedPV={}, FloatingPV={}, SwapValue={}",
                    swap.getTradeId(), fixedLegPV, floatingLegPV, swapValue);

            // Step 4: Database booking will be added later

            ProcessingResult result = ProcessingResult.success(swap.getTradeId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            return result;

        } catch (ValidationException e) {
            log.error("Validation failed for IRS {}: {}", swap.getTradeId(), e.getMessage());
            return ProcessingResult.failure(swap.getTradeId(), ProcessingStatus.VALIDATION_FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Processing failed for IRS {}: {}", swap.getTradeId(), e.getMessage(), e);
            return ProcessingResult.failure(swap.getTradeId(), ProcessingStatus.PROCESSING_FAILED, e.getMessage());
        }
    }

    private BigDecimal calculateFixedLegPV(InterestRateSwap swap) {
        // Simplified: Notional × FixedRate × Estimated Duration
        // Real calculation would use discount factors, day count conventions, etc.
        return swap.getNotional()
                .multiply(swap.getFixedRate().divide(BigDecimal.valueOf(100)))
                .multiply(BigDecimal.valueOf(5)); // Assume 5-year duration
    }

    private BigDecimal calculateFloatingLegPV(InterestRateSwap swap, BigDecimal currentRate) {
        // Simplified: Notional × (CurrentRate + Spread) × Estimated Duration
        BigDecimal spreadDecimal = swap.getFloatingSpreadBps() != null
                ? BigDecimal.valueOf(swap.getFloatingSpreadBps()).divide(BigDecimal.valueOf(10000))
                : BigDecimal.ZERO;

        BigDecimal totalRate = currentRate.divide(BigDecimal.valueOf(100)).add(spreadDecimal);

        return swap.getNotional()
                .multiply(totalRate)
                .multiply(BigDecimal.valueOf(5)); // Assume 5-year duration
    }
}