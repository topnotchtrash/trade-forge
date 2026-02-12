package com.traderecon.forge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EnrichmentService {

    private static final BigDecimal SOFR_RATE = new BigDecimal("5.30");
    private static final BigDecimal LIBOR_RATE = new BigDecimal("5.50");
    private static final BigDecimal EURIBOR_RATE = new BigDecimal("3.90");

    private static final Map<String, BigDecimal> EQUITY_PRICES = Map.of(
            "AAPL", new BigDecimal("185.50"),
            "MSFT", new BigDecimal("378.20"),
            "GOOGL", new BigDecimal("142.80"),
            "SPX", new BigDecimal("4700.00"),
            "TSLA", new BigDecimal("208.50")
    );

    private static final Map<String, BigDecimal> FX_RATES = Map.of(
            "EUR/USD", new BigDecimal("1.0850"),
            "GBP/USD", new BigDecimal("1.2650"),
            "USD/JPY", new BigDecimal("150.00"),
            "USD/CHF", new BigDecimal("0.8750"),
            "AUD/USD", new BigDecimal("0.6550")
    );

    private static final Map<String, String> COUNTERPARTY_TIERS;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("Goldman Sachs", "TIER_1");
        map.put("JP Morgan", "TIER_1");
        map.put("Morgan Stanley", "TIER_1");
        map.put("Citigroup", "TIER_1");
        map.put("Bank of America", "TIER_2");
        map.put("Barclays", "TIER_2");
        map.put("Deutsche Bank", "TIER_2");
        map.put("UBS", "TIER_2");
        map.put("Credit Suisse", "TIER_2");
        map.put("BNP Paribas", "TIER_3");
        map.put("Societe Generale", "TIER_3");
        map.put("HSBC", "TIER_3");
        map.put("RBC", "TIER_3");
        COUNTERPARTY_TIERS = Collections.unmodifiableMap(map);
    }
    public BigDecimal getSofrRate() {
        return SOFR_RATE;
    }

    public BigDecimal getLiborRate() {
        return LIBOR_RATE;
    }

    public BigDecimal getEuriborRate() {
        return EURIBOR_RATE;
    }

    public BigDecimal getRateByIndex(String index) {
        return switch (index.toUpperCase()) {
            case "SOFR" -> SOFR_RATE;
            case "LIBOR" -> LIBOR_RATE;
            case "EURIBOR" -> EURIBOR_RATE;
            default -> new BigDecimal("5.00");
        };
    }

    public BigDecimal getEquityPrice(String ticker) {
        BigDecimal price = EQUITY_PRICES.getOrDefault(ticker, new BigDecimal("100.00"));
        log.debug("Fetching equity price for {}: {}", ticker, price);
        return price;
    }

    public BigDecimal getFxRate(String currencyPair) {
        BigDecimal rate = FX_RATES.getOrDefault(currencyPair, new BigDecimal("1.00"));
        log.debug("Fetching FX rate for {}: {}", currencyPair, rate);
        return rate;
    }

    public String getCounterpartyTier(String counterparty) {
        String tier = COUNTERPARTY_TIERS.getOrDefault(counterparty, "TIER_3");
        log.debug("Counterparty {} is {}", counterparty, tier);
        return tier;
    }

    public BigDecimal getSpread(String counterparty, BigDecimal notional) {
        String tier = getCounterpartyTier(counterparty);

        int baseSpread = switch(tier) {
            case "TIER_1" -> 50;
            case "TIER_2" -> 100;
            case "TIER_3" -> 200;
            default -> 150;
        };

        if (notional.compareTo(new BigDecimal("100000000")) > 0) {
            baseSpread -= 25;
        }

        log.debug("Calculated spread for {} ({}): {} bps", counterparty, tier, baseSpread);
        return new BigDecimal(baseSpread);
    }
}