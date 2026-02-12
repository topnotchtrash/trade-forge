package com.traderecon.forge.processor;

import com.traderecon.forge.model.TradeRecord;
import com.traderecon.forge.service.DatabaseService;
import com.traderecon.forge.service.TradeMapper;
import io.annapurna.model.EquityOption;
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
import java.time.temporal.ChronoUnit;

/**
 * Processor for Equity Option trades.
 */
@Component
@Slf4j
public class OptionProcessor implements TradeProcessor {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final TradeMapper tradeMapper;
    private final DatabaseService databaseService;

    @Autowired
    public OptionProcessor(ValidationService validationService, EnrichmentService enrichmentService, DatabaseService databaseService, TradeMapper tradeMapper) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.tradeMapper = tradeMapper;
        this.databaseService = databaseService;
    }

    @Override
    public boolean supports(TradeType type) {
        return type == TradeType.EQUITY_OPTION;
    }

    @Override
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();
        EquityOption option = (EquityOption) trade;

        try {
            // Step 1: Validate
            log.debug("Validating Equity Option: {}", option.getTradeId());
            validationService.validateEquityOption(option);

            // Step 2: Enrich
            log.debug("Enriching Equity Option: {}", option.getTradeId());
            BigDecimal currentPrice = enrichmentService.getEquityPrice(option.getUnderlyingAsset());
            BigDecimal spread = enrichmentService.getSpread(option.getCounterparty(), option.getNotional());

            // Step 3: Business Logic
            log.debug("Calculating option greeks for: {}", option.getTradeId());
            long daysToExpiry = ChronoUnit.DAYS.between(option.getTradeDate(), option.getExpiryDate());
            BigDecimal intrinsicValue = calculateIntrinsicValue(option, currentPrice);
            BigDecimal timeValue = option.getPremium().subtract(intrinsicValue);
            BigDecimal delta = calculateDelta(option, currentPrice, daysToExpiry);

            log.info("Processed Option {}: Type={}, Strike={}, Spot={}, Intrinsic={}, TimeValue={}, Delta={}",
                    option.getTradeId(), option.getOptionType(), option.getStrikePrice(),
                    currentPrice, intrinsicValue, timeValue, delta);

            // Step 4: DB booking with rollback
            TradeRecord record = tradeMapper.toRecord(option);
            databaseService.bookTradeWithRollback(record);

            ProcessingResult result = ProcessingResult.success(option.getTradeId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (ValidationException e) {
            log.error("Validation failed for Option {}: {}", option.getTradeId(), e.getMessage());
            return ProcessingResult.failure(option.getTradeId(), ProcessingStatus.VALIDATION_FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Processing failed for Option {}: {}", option.getTradeId(), e.getMessage(), e);
            return ProcessingResult.failure(option.getTradeId(), ProcessingStatus.PROCESSING_FAILED, e.getMessage());
        }
    }

    private BigDecimal calculateIntrinsicValue(EquityOption option, BigDecimal spotPrice) {
        // CALL: max(Spot - Strike, 0)
        // PUT: max(Strike - Spot, 0)
        if ("CALL".equalsIgnoreCase(option.getOptionType())) {
            BigDecimal diff = spotPrice.subtract(option.getStrikePrice());
            return diff.max(BigDecimal.ZERO);
        } else {
            BigDecimal diff = option.getStrikePrice().subtract(spotPrice);
            return diff.max(BigDecimal.ZERO);
        }
    }

    private BigDecimal calculateDelta(EquityOption option, BigDecimal spotPrice, long daysToExpiry) {
        // Simplified delta calculation
        // Real implementation would use Black-Scholes

        BigDecimal moneyness = spotPrice.divide(option.getStrikePrice(), 4, BigDecimal.ROUND_HALF_UP);

        if ("CALL".equalsIgnoreCase(option.getOptionType())) {
            // Deep ITM call → delta approaches 1.0
            // ATM call → delta around 0.5
            // OTM call → delta approaches 0.0
            if (moneyness.compareTo(new BigDecimal("1.1")) > 0) return new BigDecimal("0.85");
            if (moneyness.compareTo(new BigDecimal("0.9")) < 0) return new BigDecimal("0.15");
            return new BigDecimal("0.50");
        } else {
            // PUT delta is negative
            if (moneyness.compareTo(new BigDecimal("1.1")) > 0) return new BigDecimal("-0.15");
            if (moneyness.compareTo(new BigDecimal("0.9")) < 0) return new BigDecimal("-0.85");
            return new BigDecimal("-0.50");
        }
    }
}