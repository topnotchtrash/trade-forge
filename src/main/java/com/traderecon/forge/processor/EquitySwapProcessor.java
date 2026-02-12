package com.traderecon.forge.processor;

import com.traderecon.forge.model.TradeRecord;
import com.traderecon.forge.service.DatabaseService;
import com.traderecon.forge.service.TradeMapper;
import io.annapurna.model.EquitySwap;
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
 * Processor for Equity Swap trades.
 */
@Component
@Slf4j
public class EquitySwapProcessor implements TradeProcessor {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final TradeMapper tradeMapper;
    private final DatabaseService databaseService;
    @Autowired
    public EquitySwapProcessor(ValidationService validationService, EnrichmentService enrichmentService, TradeMapper tradeMapper, DatabaseService databaseService) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.databaseService  = databaseService;
        this.tradeMapper = tradeMapper;
    }

    @Override
    public boolean supports(TradeType type) {
        return type == TradeType.EQUITY_SWAP;
    }

    @Override
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();
        EquitySwap swap = (EquitySwap) trade;

        try {
            // Step 1: Validate
            log.debug("Validating Equity Swap: {}", swap.getTradeId());
            validationService.validateEquitySwap(swap);

            // Step 2: Enrich
            log.debug("Enriching Equity Swap: {}", swap.getTradeId());
            BigDecimal currentPrice = enrichmentService.getEquityPrice(swap.getReferenceAsset());
            BigDecimal spread = enrichmentService.getSpread(swap.getCounterparty(), swap.getNotional());

            // Step 3: Business Logic
            log.debug("Calculating equity swap metrics for: {}", swap.getTradeId());
            BigDecimal equityLegValue = calculateEquityLegValue(swap, currentPrice);
            BigDecimal fundingLegValue = calculateFundingLegValue(swap);
            BigDecimal swapValue = equityLegValue.subtract(fundingLegValue);

            log.info("Processed Equity Swap {}: EquityLegValue={}, FundingLegValue={}, SwapValue={}",
                    swap.getTradeId(), equityLegValue, fundingLegValue, swapValue);

            // Step 4: DB booking with rollback
            TradeRecord record = tradeMapper.toRecord(swap);
            databaseService.bookTradeWithRollback(record);

            ProcessingResult result = ProcessingResult.success(swap.getTradeId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (ValidationException e) {
            log.error("Validation failed for Equity Swap {}: {}", swap.getTradeId(), e.getMessage());
            return ProcessingResult.failure(swap.getTradeId(), ProcessingStatus.VALIDATION_FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Processing failed for Equity Swap {}: {}", swap.getTradeId(), e.getMessage(), e);
            return ProcessingResult.failure(swap.getTradeId(), ProcessingStatus.PROCESSING_FAILED, e.getMessage());
        }
    }

    private BigDecimal calculateEquityLegValue(EquitySwap swap, BigDecimal currentPrice) {
        // Simplified equity return calculation
        // Assumes notional represents number of shares × initial price
        // Real calculation would track actual equity performance
        return swap.getNotional().multiply(BigDecimal.valueOf(0.08)); // Assume 8% equity return
    }

    private BigDecimal calculateFundingLegValue(EquitySwap swap) {
        // Simplified funding cost
        // Real calculation: SOFR + spread over the period
        BigDecimal sofrRate = enrichmentService.getSofrRate();
        return swap.getNotional()
                .multiply(sofrRate.divide(BigDecimal.valueOf(100)))
                .multiply(BigDecimal.valueOf(0.5)); // Assume 6 months
    }
}