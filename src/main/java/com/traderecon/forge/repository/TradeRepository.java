package com.traderecon.forge.repository;

import com.traderecon.forge.model.TradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for TradeRecord entity.
 *
 * Provides database access for trade records.
 * Note: In simulation mode, all writes are rolled back.
 */
@Repository
public interface TradeRepository extends JpaRepository<TradeRecord, Long> {

    /**
     * Find trade by trade ID.
     */
    Optional<TradeRecord> findByTradeId(String tradeId);

    /**
     * Check if trade exists by trade ID.
     */
    boolean existsByTradeId(String tradeId);
}