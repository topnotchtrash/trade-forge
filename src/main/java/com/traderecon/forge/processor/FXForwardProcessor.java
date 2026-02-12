package com.traderecon.forge.processor;

import com.traderecon.forge.model.TradeRecord;
import com.traderecon.forge.service.DatabaseService;
import com.traderecon.forge.service.TradeMapper;
import io.annapurna.model.FXForward;
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
 * Processor for FX Forward trades.
 */
@Component
@Slf4j
public class FXForwardProcessor implements TradeProcessor {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final TradeMapper tradeMapper;
    private final DatabaseService databaseService;
    @Autowired
    public FXForwardProcessor(ValidationService validationService, EnrichmentService enrichmentService, TradeMapper tradeMapper,DatabaseService databaseService ) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.databaseService = databaseService;
        this.tradeMapper = tradeMapper;
    }

    @Override
    public boolean supports(TradeType type) {
        return type == TradeType.FX_FORWARD;
    }

    @Override
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();
        FXForward forward = (FXForward) trade;

        try {
            // Step 1: Validate
            log.debug("Validating FX Forward: {}", forward.getTradeId());
            validationService.validateFXForward(forward);

            // Step 2: Enrich
            log.debug("Enriching FX Forward: {}", forward.getTradeId());
            BigDecimal spotRate = enrichmentService.getFxRate(forward.getCurrencyPair());
            BigDecimal spread = enrichmentService.getSpread(forward.getCounterparty(), forward.getNotional());

            // Step 3: Business Logic
            log.debug("Calculating FX forward MTM for: {}", forward.getTradeId());
            long daysToMaturity = ChronoUnit.DAYS.between(forward.getTradeDate(), forward.getMaturityDate());
            BigDecimal forwardPoints = calculateForwardPoints(spotRate, daysToMaturity);
            BigDecimal theoreticalForward = spotRate.add(forwardPoints);
            BigDecimal mtm = calculateMTM(forward, spotRate, theoreticalForward);

            log.info("Processed FX Forward {}: Spot={}, Forward={}, TheoreticalForward={}, MTM={}",
                    forward.getTradeId(), spotRate, forward.getForwardRate(), theoreticalForward, mtm);

            // Step 4: DB booking with rollback
            TradeRecord record = tradeMapper.toRecord(forward);
            databaseService.bookTradeWithRollback(record);

            ProcessingResult result = ProcessingResult.success(forward.getTradeId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (ValidationException e) {
            log.error("Validation failed for FX Forward {}: {}", forward.getTradeId(), e.getMessage());
            return ProcessingResult.failure(forward.getTradeId(), ProcessingStatus.VALIDATION_FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Processing failed for FX Forward {}: {}", forward.getTradeId(), e.getMessage(), e);
            return ProcessingResult.failure(forward.getTradeId(), ProcessingStatus.PROCESSING_FAILED, e.getMessage());
        }
    }

    private BigDecimal calculateForwardPoints(BigDecimal spotRate, long daysToMaturity) {
        // Simplified forward points = spot × interest rate differential × days/360
        // Assume 2% interest rate differential
        BigDecimal rateDiff = new BigDecimal("0.02");
        return spotRate.multiply(rateDiff)
                .multiply(BigDecimal.valueOf(daysToMaturity))
                .divide(BigDecimal.valueOf(360), 6, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculateMTM(FXForward forward, BigDecimal spotRate, BigDecimal theoreticalForward) {
        // MTM = Notional × (TheoreticalForward - ContractForward)
        BigDecimal rateDiff = theoreticalForward.subtract(forward.getForwardRate());
        return forward.getNotional().multiply(rateDiff);
    }
}