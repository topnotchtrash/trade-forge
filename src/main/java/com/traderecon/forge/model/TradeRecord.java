package com.traderecon.forge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", unique = true, nullable = false)
    private String tradeId;

    @Column(name = "trade_type", nullable = false)
    private String tradeType;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "counterparty", nullable = false)
    private String counterparty;

    @Column(name = "notional", nullable = false, precision = 18, scale = 2)
    private BigDecimal notional;

    @Column(name = "currency", nullable = false)
    private String currency;

    // Interest Rate Swap fields
    @Column(name = "fixed_rate", precision = 10, scale = 4)
    private BigDecimal fixedRate;

    @Column(name = "floating_rate_index")
    private String floatingRateIndex;

    @Column(name = "floating_spread_bps")
    private Integer floatingSpreadBps;

    @Column(name = "direction")
    private String direction;

    // Equity Swap fields
    @Column(name = "reference_asset")
    private String referenceAsset;

    @Column(name = "return_type")
    private String returnType;

    @Column(name = "funding_leg")
    private String fundingLeg;

    // FX Forward fields
    @Column(name = "currency_pair")
    private String currencyPair;

    @Column(name = "forward_rate", precision = 18, scale = 6)
    private BigDecimal forwardRate;

    // Option fields
    @Column(name = "option_type")
    private String optionType;

    @Column(name = "strike_price", precision = 18, scale = 2)
    private BigDecimal strikePrice;

    @Column(name = "premium", precision = 18, scale = 2)
    private BigDecimal premium;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "underlying_asset")
    private String underlyingAsset;

    // CDS fields
    @Column(name = "reference_entity")
    private String referenceEntity;

    @Column(name = "spread_bps")
    private Integer spreadBps;

    @Column(name = "recovery_rate")
    private Integer recoveryRate;

    // Audit fields
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_duration_ms")
    private Integer processingDurationMs;

    @Column(name = "status")
    private String status;
}