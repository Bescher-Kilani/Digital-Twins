package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

/**
 * Spring configuration for MongoDB custom conversions.
 * <p>
 * Registers converters for mapping between Jackson {@code JsonNode} and MongoDB {@code Document}.
 * </p>
 */
@Configuration
public class MongoConfig {

    /**
     * Registers custom read and write converters for MongoDB.
     *
     * @param readConverter  converts {@code Document} to {@code JsonNode}
     * @param writeConverter converts {@code JsonNode} to {@code Document}
     * @return configured {@code MongoCustomConversions} bean
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions(
            JsonNodeReadConverter readConverter,
            JsonNodeWriteConverter writeConverter
    ) {
        return new MongoCustomConversions(List.of(readConverter, writeConverter));
    }
}