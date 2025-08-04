package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for customizing Jackson (de)serialization.
 * <p>
 * Registers {@link JacksonAASModule} to enable correct handling of AAS4j model types and enums.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Provides the {@link JacksonAASModule} bean for Jackson customization.
     *
     * @return configured module for AAS4j support
     */
    @Bean
    public SimpleModule jacksonAASModule() {
        return new JacksonAASModule();
    }
}
