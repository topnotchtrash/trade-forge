package com.traderecon.forge.processor;

import io.annapurna.model.Trade;
import io.annapurna.model.TradeType;
import com.traderecon.forge.model.ProcessingResult;

/**
 * Interface for trade processors.
 *
 * Each trade type (SWAP, EQUITY_SWAP, FX_FORWARD, OPTION, CDS) has its own
 * processor implementation that handles validation, enrichment, business logic,
 * and database booking.
 */
public interface TradeProcessor {

    /**
     * Check if this processor supports the given trade type.
     *
     * @param type The trade type
     * @return true if this processor handles the trade type
     */
    boolean supports(TradeType type);

    /**
     * Process a trade through the complete workflow:
     * 1. Validation
     * 2. Enrichment
     * 3. Business logic
     * 4. Database booking (with rollback)
     *
     * @param trade The trade to process
     * @return Processing result (success/failure + metrics)
     */
    ProcessingResult process(Trade trade);
}