package com.traderecon.forge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Trade-Forge Application
 *
 * Kafka consumer service that processes synthetic trades in parallel,
 * validates business logic, and simulates database booking with transaction rollback.
 *
 * Part of the trade-reconciliation-system project.
 */
@SpringBootApplication
public class TradeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeForgeApplication.class, args);
    }
}