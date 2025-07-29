package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Configuration class for Spring Security settings.
 * <p>
 * This configuration enables OAuth2 resource server support and defines access rules
 * for guest and authenticated endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain for HTTP requests.
     *
     * <p>Key configurations:
     * <ul>
     *   <li>Allows unrestricted access to all endpoints under {@code /guest/**}.</li>
     *   <li>Requires authentication for all other endpoints.</li>
     *   <li>Disables CSRF protection (useful for stateless REST APIs).</li>
     *   <li>Enables JWT-based OAuth2 resource server authentication.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs while building the security configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/guest/**", "/submodels/templates").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
