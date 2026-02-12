package com.traderecon.forge.service;

import io.annapurna.model.*;
import com.traderecon.forge.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ValidationService {

    private static final BigDecimal MAX_NOTIONAL = new BigDecimal("10000000000"); // $10 billion
    private static final List<String> VALID_CURRENCIES = Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"
    );

    public void validateCommonFields(Trade trade) {
        if (trade.getTradeId() == null || trade.getTradeId().trim().isEmpty()) {
            throw new ValidationException("Trade ID cannot be null or empty");
        }

        if (trade.getTradeDate() == null) {
            throw new ValidationException("Trade date cannot be null");
        }

        if (trade.getTradeDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Trade date cannot be in the future");
        }

        if (trade.getSettlementDate() == null) {
            throw new ValidationException("Settlement date cannot be null");
        }

        if (trade.getSettlementDate().isBefore(trade.getTradeDate())) {
            throw new ValidationException("Settlement date must be on or after trade date");
        }

        if (trade.getCounterparty() == null || trade.getCounterparty().trim().isEmpty()) {
            throw new ValidationException("Counterparty cannot be null or empty");
        }

        if (trade.getNotional() == null || trade.getNotional().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Notional must be greater than zero");
        }

        if (trade.getNotional().compareTo(MAX_NOTIONAL) > 0) {
            throw new ValidationException("Notional exceeds maximum allowed: " + MAX_NOTIONAL);
        }

        if (trade.getCurrency() == null || !VALID_CURRENCIES.contains(trade.getCurrency().toString())) {
            throw new ValidationException("Invalid currency: " + trade.getCurrency());
        }
    }

    public void validateInterestRateSwap(InterestRateSwap swap) {
        validateCommonFields(swap);

        if (swap.getFixedRate() == null || swap.getFixedRate().compareTo(BigDecimal.ZERO) < 0
                || swap.getFixedRate().compareTo(new BigDecimal("20")) > 0) {
            throw new ValidationException("Fixed rate must be between 0% and 20%");
        }

        if (swap.getFloatingRateIndex() == null || swap.getFloatingRateIndex().trim().isEmpty()) {
            throw new ValidationException("Floating rate index cannot be null or empty");
        }

        if (swap.getDirection() == null || swap.getDirection().trim().isEmpty()) {
            throw new ValidationException("Direction cannot be null or empty");
        }

        if (swap.getEffectiveDate() != null && swap.getEffectiveDate().isBefore(swap.getTradeDate())) {
            throw new ValidationException("Effective date cannot be before trade date");
        }
    }

    public void validateEquitySwap(EquitySwap swap) {
        validateCommonFields(swap);

        if (swap.getReferenceAsset() == null || swap.getReferenceAsset().trim().isEmpty()) {
            throw new ValidationException("Reference asset cannot be null or empty");
        }

        if (swap.getReturnType() == null) {
            throw new ValidationException("Return type cannot be null");
        }

        if (swap.getFundingLeg() == null) {
            throw new ValidationException("Funding leg cannot be null");
        }

    }

    public void validateFXForward(FXForward forward) {
        validateCommonFields(forward);

        if (forward.getCurrencyPair() == null || forward.getCurrencyPair().trim().isEmpty()) {
            throw new ValidationException("Currency pair cannot be null or empty");
        }

        if (!forward.getCurrencyPair().matches("^[A-Z]{3}/[A-Z]{3}$")) {
            throw new ValidationException("Invalid currency pair format. Expected XXX/YYY");
        }

        if (forward.getForwardRate() == null || forward.getForwardRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Forward rate must be greater than zero");
        }

        if (forward.getMaturityDate() == null) {
            throw new ValidationException("Maturity date cannot be null");
        }

        if (forward.getMaturityDate().isBefore(forward.getTradeDate())) {
            throw new ValidationException("Maturity date must be after trade date");
        }

        if (forward.getMaturityDate().isAfter(forward.getTradeDate().plusYears(2))) {
            throw new ValidationException("Maturity date cannot be more than 2 years from trade date");
        }
    }

    public void validateEquityOption(EquityOption option) {
        validateCommonFields(option);

        if (option.getOptionType() == null) {
            throw new ValidationException("Option type cannot be null");
        }

        if (option.getStrikePrice() == null || option.getStrikePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Strike price must be greater than zero");
        }

        if (option.getExpiryDate() == null) {
            throw new ValidationException("Expiry date cannot be null");
        }

        if (option.getExpiryDate().isBefore(option.getTradeDate())) {
            throw new ValidationException("Expiry date must be after trade date");
        }

        if (option.getPremium() == null || option.getPremium().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Premium cannot be negative");
        }

        if (option.getUnderlyingAsset() == null || option.getUnderlyingAsset().trim().isEmpty()) {
            throw new ValidationException("Underlying asset cannot be null or empty");
        }
    }

    public void validateCreditDefaultSwap(CreditDefaultSwap cds) {
        validateCommonFields(cds);

        if (cds.getReferenceEntity() == null || cds.getReferenceEntity().trim().isEmpty()) {
            throw new ValidationException("Reference entity cannot be null or empty");
        }

        if (cds.getSpreadBps() < 0 || cds.getSpreadBps() > 10000) {
            throw new ValidationException("Spread must be between 0 and 10,000 bps");
        }

        if (cds.getMaturityDate() == null) {
            throw new ValidationException("Maturity date cannot be null");
        }

        if (cds.getMaturityDate().isBefore(cds.getTradeDate().plusYears(1))) {
            throw new ValidationException("CDS tenor must be at least 1 year");
        }

        if (cds.getMaturityDate().isAfter(cds.getTradeDate().plusYears(10))) {
            throw new ValidationException("CDS tenor cannot exceed 10 years");
        }

        if (cds.getRecoveryRate() == null ||
                cds.getRecoveryRate().compareTo(BigDecimal.ZERO) < 0 ||
                cds.getRecoveryRate().compareTo(new BigDecimal("100")) > 0) {

            throw new ValidationException("Recovery rate must be between 0 and 100");
        }
    }
}