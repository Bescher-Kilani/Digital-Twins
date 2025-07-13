package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient smtRepoWebClient() {
        return WebClient.builder()
                .baseUrl("https://smt-repo.admin-shell-io.com/api/v3.0/submodels")
                .build();
    }
}