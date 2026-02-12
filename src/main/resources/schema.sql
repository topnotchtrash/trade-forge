-- Trade-Forge Database Schema
-- Creates trades table for simulation (all writes will be rolled back)

CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(255) UNIQUE NOT NULL,
    trade_type VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    settlement_date DATE NOT NULL,
    maturity_date DATE,
    counterparty VARCHAR(255) NOT NULL,
    notional DECIMAL(18, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,

    -- Interest Rate Swap fields
    fixed_rate DECIMAL(10, 4),
    floating_rate_index VARCHAR(50),
    floating_spread_bps INTEGER,
    direction VARCHAR(50),

    -- Equity Swap fields
    reference_asset VARCHAR(50),
    return_type VARCHAR(50),
    funding_leg VARCHAR(100),

    -- FX Forward fields
    currency_pair VARCHAR(10),
    forward_rate DECIMAL(18, 6),

    -- Option fields
    option_type VARCHAR(10),
    strike_price DECIMAL(18, 2),
    premium DECIMAL(18, 2),
    expiry_date DATE,
    underlying_asset VARCHAR(50),

    -- CDS fields
    reference_entity VARCHAR(255),
    spread_bps INTEGER,
    recovery_rate INTEGER,

    -- Audit fields
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processing_duration_ms INTEGER,
    status VARCHAR(20) DEFAULT 'SUCCESS'
    );

-- Create indexes for query performance
CREATE INDEX IF NOT EXISTS idx_trade_id ON trades(trade_id);
CREATE INDEX IF NOT EXISTS idx_trade_type ON trades(trade_type);
CREATE INDEX IF NOT EXISTS idx_processed_at ON trades(processed_at);
CREATE INDEX IF NOT EXISTS idx_counterparty ON trades(counterparty);