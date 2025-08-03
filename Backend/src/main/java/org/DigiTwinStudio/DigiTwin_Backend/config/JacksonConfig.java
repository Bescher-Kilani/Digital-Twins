package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for customizing Jackson's object mapper.
 * <p>
 * Registers the custom {@link JacksonAASModule} as a bean,
 * ensuring that all Jackson (de)serialization for REST APIs uses the
 * specialized module with AAS4j mappings and deserializers.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Registers the {@link JacksonAASModule} with Spring's Jackson configuration.
     * <p>
     * This module includes custom type mappings and deserializers for
     * the Asset Administration Shell (AAS) data model, enabling dynamic
     * and robust JSON (de)serialization.
     * </p>
     *
     * @return the configured JacksonAASModule bean
     */
    @Bean
    public SimpleModule jacksonAASModule() {
        return new JacksonAASModule();
    }
}
