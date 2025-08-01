package org.DigiTwinStudio.DigiTwin_Backend.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public SimpleModule jacksonAASModule() {
        return new JacksonAASModule();
    }
}
