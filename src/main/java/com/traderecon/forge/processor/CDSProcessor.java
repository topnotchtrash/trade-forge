package com.traderecon.forge.processor;

import com.traderecon.forge.model.TradeRecord;
import com.traderecon.forge.service.DatabaseService;
import com.traderecon.forge.service.TradeMapper;
import io.annapurna.model.CreditDefaultSwap;
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
 * Processor for Credit Default Swap trades.
 */
@Component
@Slf4j
public class CDSProcessor implements TradeProcessor {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final DatabaseService databaseService;
    private final TradeMapper tradeMapper;
    @Autowired
    public CDSProcessor(ValidationService validationService, EnrichmentService enrichmentService, DatabaseService databaseService,
                        TradeMapper tradeMapper) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.databaseService = databaseService;
        this.tradeMapper = tradeMapper;
    }

    @Override
    public boolean supports(TradeType type) {
        return type == TradeType.CREDIT_DEFAULT_SWAP;
    }

    @Override
    public ProcessingResult process(Trade trade) {
        long startTime = System.currentTimeMillis();
        CreditDefaultSwap cds = (CreditDefaultSwap) trade;

        try {
            // Step 1: Validate
            log.debug("Validating CDS: {}", cds.getTradeId());
            validationService.validateCreditDefaultSwap(cds);

            // Step 2: Enrich
            log.debug("Enriching CDS: {}", cds.getTradeId());
            BigDecimal spread = enrichmentService.getSpread(cds.getCounterparty(), cds.getNotional());

            // Step 3: Business Logic
            log.debug("Calculating CDS metrics for: {}", cds.getTradeId());
            long daysToMaturity = ChronoUnit.DAYS.between(cds.getTradeDate(), cds.getMaturityDate());
            BigDecimal annualPremium = calculateAnnualPremium(cds);
            BigDecimal protectionValue = calculateProtectionValue(cds);
            BigDecimal cdsValue = protectionValue.subtract(annualPremium);

            log.info("Processed CDS {}: ReferenceEntity={}, Spread={} bps, AnnualPremium={}, ProtectionValue={}, CDSValue={}",
                    cds.getTradeId(), cds.getReferenceEntity(), cds.getSpreadBps(),
                    annualPremium, protectionValue, cdsValue);
            // Step 4: DB booking with rollback
            TradeRecord record = tradeMapper.toRecord(cds);
            databaseService.bookTradeWithRollback(record);

            ProcessingResult result = ProcessingResult.success(cds.getTradeId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (ValidationException e) {
            log.error("Validation failed for CDS {}: {}", cds.getTradeId(), e.getMessage());
            return ProcessingResult.failure(cds.getTradeId(), ProcessingStatus.VALIDATION_FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Processing failed for CDS {}: {}", cds.getTradeId(), e.getMessage(), e);
            return ProcessingResult.failure(cds.getTradeId(), ProcessingStatus.PROCESSING_FAILED, e.getMessage());
        }
    }

    private BigDecimal calculateAnnualPremium(CreditDefaultSwap cds) {
        // Annual premium = Notional × Spread (in decimal)
        BigDecimal spreadDecimal = BigDecimal.valueOf(cds.getSpreadBps()).divide(BigDecimal.valueOf(10000));
        return cds.getNotional().multiply(spreadDecimal);
    }

    private BigDecimal calculateProtectionValue(CreditDefaultSwap cds) {
        // Simplified protection leg value
        // Real calculation: (1 - RecoveryRate) × Notional × Default Probability

        // Estimate default probability from spread
        // Higher spread → higher default probability
        BigDecimal defaultProb = estimateDefaultProbability(cds.getSpreadBps());

        // Protection value = Loss Given Default × Default Probability
        BigDecimal recoveryRateDecimal = new BigDecimal(String.valueOf(cds.getRecoveryRate())).divide(BigDecimal.valueOf(100));
        BigDecimal lossGivenDefault = cds.getNotional()
                .multiply(BigDecimal.ONE.subtract(recoveryRateDecimal));
        return lossGivenDefault.multiply(defaultProb);
    }

    private BigDecimal estimateDefaultProbability(int spreadBps) {
        // Simplified: spread / 10000 as rough default probability
        // 100 bps → 1% default prob
        // 1000 bps → 10% default prob
        BigDecimal prob = BigDecimal.valueOf(spreadBps).divide(BigDecimal.valueOf(10000));

        // Cap at 50%
        return prob.min(new BigDecimal("0.50"));
    }
}