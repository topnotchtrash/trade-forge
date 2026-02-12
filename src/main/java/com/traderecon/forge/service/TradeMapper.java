package com.traderecon.forge.service;

import io.annapurna.model.*;
import com.traderecon.forge.model.TradeRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Maps Trade domain objects to TradeRecord entities for database persistence.
 */
@Component
public class TradeMapper {

    /**
     * Map any Trade type to TradeRecord.
     */
    public TradeRecord toRecord(Trade trade) {
        TradeRecord record = TradeRecord.builder()
                .tradeId(trade.getTradeId())
                .tradeType(trade.getTradeType().toString())
                .tradeDate(trade.getTradeDate())
                .settlementDate(trade.getSettlementDate())
                .maturityDate(trade.getMaturityDate())
                .counterparty(trade.getCounterparty())
                .notional(trade.getNotional())
                .currency(trade.getCurrency().toString())
                .processedAt(LocalDateTime.now())
                .status("SUCCESS")
                .build();

        // Map type-specific fields
        if (trade instanceof InterestRateSwap) {
            mapInterestRateSwap((InterestRateSwap) trade, record);
        } else if (trade instanceof EquitySwap) {
            mapEquitySwap((EquitySwap) trade, record);
        } else if (trade instanceof FXForward) {
            mapFXForward((FXForward) trade, record);
        } else if (trade instanceof EquityOption) {
            mapEquityOption((EquityOption) trade, record);
        } else if (trade instanceof CreditDefaultSwap) {
            mapCreditDefaultSwap((CreditDefaultSwap) trade, record);
        }

        return record;
    }

    private void mapInterestRateSwap(InterestRateSwap swap, TradeRecord record) {
        record.setFixedRate(swap.getFixedRate());
        record.setFloatingRateIndex(swap.getFloatingRateIndex());
        record.setFloatingSpreadBps(swap.getFloatingSpreadBps());
        record.setDirection(swap.getDirection());
    }

    private void mapEquitySwap(EquitySwap swap, TradeRecord record) {
        record.setReferenceAsset(swap.getReferenceAsset());
        record.setReturnType(swap.getReturnType());
        record.setFundingLeg(swap.getFundingLeg());
    }

    private void mapFXForward(FXForward forward, TradeRecord record) {
        record.setCurrencyPair(forward.getCurrencyPair());
        record.setForwardRate(forward.getForwardRate());
    }

    private void mapEquityOption(EquityOption option, TradeRecord record) {
        record.setOptionType(option.getOptionType());
        record.setStrikePrice(option.getStrikePrice());
        record.setPremium(option.getPremium());
        record.setExpiryDate(option.getExpiryDate());
        record.setUnderlyingAsset(option.getUnderlyingAsset());
    }

    private void mapCreditDefaultSwap(CreditDefaultSwap cds, TradeRecord record) {
        record.setReferenceEntity(cds.getReferenceEntity());
        record.setSpreadBps(cds.getSpreadBps());

        // Convert BigDecimal to Integer
        if (cds.getRecoveryRate() != null) {
            record.setRecoveryRate(cds.getRecoveryRate().intValue());
        }
    }
}