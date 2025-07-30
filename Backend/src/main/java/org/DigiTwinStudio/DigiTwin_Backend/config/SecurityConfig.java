package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

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
     *   <li>Configures CORS to allow frontend requests.</li>
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Configures CORS settings for the application.
     *
     * <p>This configuration allows:
     * <ul>
     *   <li>Requests from the frontend running on localhost:5173</li>
     *   <li>All HTTP methods (GET, POST, PUT, DELETE, OPTIONS)</li>
     *   <li>All headers</li>
     *   <li>Credentials to be included in requests</li>
     * </ul>
     *
     * @return the {@link CorsConfigurationSource} with the defined settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
