package com.traderecon.forge.model;

/**
 * Status of trade processing.
 */
public enum ProcessingStatus {
    SUCCESS,           // Trade processed successfully
    VALIDATION_FAILED, // Failed validation checks
    ENRICHMENT_FAILED, // Failed to enrich data
    PROCESSING_FAILED, // Business logic error
    DATABASE_FAILED,   // Database operation error
    TIMEOUT            // Processing timeout
}