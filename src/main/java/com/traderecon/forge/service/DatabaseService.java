package com.traderecon.forge.service;

import com.traderecon.forge.exception.ProcessingException;
import com.traderecon.forge.model.TradeRecord;
import com.traderecon.forge.repository.TradeRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Service for database operations with transaction rollback.
 *
 * In simulation mode: All writes are executed inside transactions
 * but are ALWAYS rolled back to avoid persisting data.
 */
@Service
@Slf4j
public class DatabaseService {

    private final TradeRepository tradeRepository;
    private final PlatformTransactionManager transactionManager;
    private final boolean simulationMode;

    @Autowired
    public DatabaseService(
            TradeRepository tradeRepository,
            PlatformTransactionManager transactionManager,
            @Value("${processing.simulation-mode:true}")
            boolean simulationMode
    ) {
        this.tradeRepository = tradeRepository;
        this.transactionManager = transactionManager;
        this.simulationMode = simulationMode;

        log.info("DatabaseService initialized in {} mode",
                simulationMode ? "SIMULATION (rollback)" : "PRODUCTION (commit)");
    }

    public void bookTradeWithRollback(TradeRecord record) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("TradeBookingTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setTimeout(10);

        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            tradeRepository.save(record);

            log.info("Booked trade: {} (notional: {}, counterparty: {})",
                    record.getTradeId(), record.getNotional(), record.getCounterparty());

            if (simulationMode) {
                // ROLLBACK in simulation mode
                transactionManager.rollback(status);
                log.debug("Transaction rolled back (simulation mode): {}", record.getTradeId());
            } else {
                // COMMIT in production mode
                transactionManager.commit(status);
                log.info("Transaction committed (production mode): {}", record.getTradeId());
            }

        } catch (Exception e) {
            log.error("Database error for trade: {}", record.getTradeId(), e);
            transactionManager.rollback(status);
            throw new ProcessingException("Database booking failed", e);
        }
    }
}