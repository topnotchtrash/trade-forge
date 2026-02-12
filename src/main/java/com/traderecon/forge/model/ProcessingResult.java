package com.traderecon.forge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of trade processing.
 *
 * Contains status, timing, and error details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {

    private String tradeId;
    private ProcessingStatus status;
    private String errorMessage;
    private long processingTimeMs;
    private LocalDateTime processedAt;

    /**
     * Create a successful processing result.
     */
    public static ProcessingResult success(String tradeId) {
        return ProcessingResult.builder()
                .tradeId(tradeId)
                .status(ProcessingStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a failed processing result.
     */
    public static ProcessingResult failure(String tradeId, ProcessingStatus status, String errorMessage) {
        return ProcessingResult.builder()
                .tradeId(tradeId)
                .status(status)
                .errorMessage(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a timeout result.
     */
    public static ProcessingResult timeout(String tradeId) {
        return ProcessingResult.builder()
                .tradeId(tradeId)
                .status(ProcessingStatus.TIMEOUT)
                .errorMessage("Processing timeout exceeded")
                .processedAt(LocalDateTime.now())
                .build();
    }
}