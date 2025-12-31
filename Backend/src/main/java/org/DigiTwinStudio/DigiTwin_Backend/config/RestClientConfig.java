package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for RestClient beans.
 * Replaces WebClient to avoid Netty event loop keeping the service awake on Railway.
 */
@Configuration
public class RestClientConfig {

    /**
     * Configures a {@link RestClient} for the SMT-Repo API.
     *
     * Note: RestClient uses the standard HTTP client which doesn't have the same
     * memory buffer limitations as WebClient. The 10MB limit from WebClient
     * is not needed here as RestClient handles large responses differently.
     *
     * @return the configured RestClient instance
     */
    @Bean
    public RestClient smtRepoRestClient() {
        // Configure timeouts to avoid hanging connections
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);  // 10 seconds
        requestFactory.setReadTimeout(30000);     // 30 seconds

        return RestClient.builder()
                .baseUrl("https://smt-repo.admin-shell-io.com/api/v3.0/submodels")
                .requestFactory(requestFactory)
                .build();
    }
}