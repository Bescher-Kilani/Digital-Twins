package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions(
            JsonNodeReadConverter readConverter,
            JsonNodeWriteConverter writeConverter
    ) {
        return new MongoCustomConversions(List.of(readConverter, writeConverter));
    }
}