package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for WebClient beans.
 */
@Configuration
public class WebClientConfig {

    /**
     * Configures a {@link WebClient} for the SMT-Repo API with increased memory limit.
     *
     * @return the configured WebClient instance
     */
    @Bean
    public WebClient smtRepoWebClient() {
        return WebClient.builder()
                .baseUrl("https://smt-repo.admin-shell-io.com/api/v3.0/submodels")
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)  // 10 MB. Needed because a Spring Boot standard is 256 KB but the Response is at least 262KB big
                )
                .build();
    }
}
