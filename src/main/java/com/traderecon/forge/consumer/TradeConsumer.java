package com.traderecon.forge.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.annapurna.model.*;
import com.traderecon.forge.model.ProcessingResult;
import com.traderecon.forge.service.TradeProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeConsumer {

    private final ObjectMapper objectMapper;
    private final TradeProcessingService processingService;

    @Autowired
    public TradeConsumer(ObjectMapper objectMapper, TradeProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${kafka.topic.trade-input}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void consumeTrade(
            @Payload String tradeJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            // Deserialize JSON to Trade object
            Trade trade = objectMapper.readValue(tradeJson, Trade.class);

            // WORKAROUND: tradeType field is @JsonIgnore in Annapurna
            // Manually set it based on concrete type
            setTradeType(trade);

            log.info("Received trade from partition {}, offset {}: ID={}, Type={}",
                    partition, offset, trade.getTradeId(), trade.getTradeType());

            // Process trade
            ProcessingResult result = processingService.process(trade);

            // Log result
            log.info("Processed trade {}: Status={}, Duration={}ms",
                    trade.getTradeId(), result.getStatus(), result.getProcessingTimeMs());

            // Acknowledge message (commit offset)
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process trade from partition {}, offset {}: {}",
                    partition, offset, e.getMessage(), e);

            // DO NOT acknowledge - message will be retried
        }
    }

    private void setTradeType(Trade trade) {
        if (trade instanceof InterestRateSwap) {
            trade.setTradeType(TradeType.INTEREST_RATE_SWAP);
        } else if (trade instanceof EquitySwap) {
            trade.setTradeType(TradeType.EQUITY_SWAP);
        } else if (trade instanceof FXForward) {
            trade.setTradeType(TradeType.FX_FORWARD);
        } else if (trade instanceof EquityOption) {
            trade.setTradeType(TradeType.EQUITY_OPTION);
        } else if (trade instanceof CreditDefaultSwap) {
            trade.setTradeType(TradeType.CREDIT_DEFAULT_SWAP);
        }
    }
}