package com.traderecon.forge.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.annapurna.model.CreditDefaultSwap;
import io.annapurna.model.EquityOption;
import io.annapurna.model.EquitySwap;
import io.annapurna.model.FXForward;
import io.annapurna.model.InterestRateSwap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
/**
 * Kafka Consumer Configuration
 *
 * Configures ObjectMapper for deserializing polymorphic Trade types from JSON.
 * Registers all trade subtypes for proper Jackson deserialization.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Configure ObjectMapper for Trade deserialization.
     *
     * Handles:
     * - Polymorphic types (InterestRateSwap, EquitySwap, etc.)
     * - Java 8 date/time (LocalDate)
     * - Pretty printing for debugging
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable enum deserialization by string value
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        mapper.registerSubtypes(
                new NamedType(InterestRateSwap.class, "INTEREST_RATE_SWAP"),
                new NamedType(EquitySwap.class, "EQUITY_SWAP"),
                new NamedType(FXForward.class, "FX_FORWARD"),
                new NamedType(EquityOption.class, "EQUITY_OPTION"),
                new NamedType(CreditDefaultSwap.class, "CREDIT_DEFAULT_SWAP")
        );

        return mapper;
    }
}