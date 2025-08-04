package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configures Spring Security for OAuth2/JWT authentication with Keycloak.
 * <p>
 * Sets up public and protected API routes, CORS for frontend access, and a custom JWT decoder for Docker compatibility.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Defines access rules, enables JWT authentication, disables CSRF, and configures CORS.
     *
     * @param http the HTTP security configuration
     * @return the configured security filter chain
     * @throws Exception if a configuration error occurs
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/guest/**", "/marketplace/**", "/submodels/templates").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
                .build();
    }

    /**
     * Custom JWT decoder that accepts multiple issuer URIs for Docker compatibility.
     *
     * @return configured JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuerUri + "/protocol/openid-connect/certs").build();

        OAuth2TokenValidator<Jwt> issuerValidator = new OAuth2TokenValidator<Jwt>() {
            @Override
            public OAuth2TokenValidatorResult validate(Jwt jwt) {
                String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
                List<String> validIssuers = Arrays.asList(
                        "http://localhost:8080/realms/DigiTwinStudio",  // Frontend perspective
                        "http://keycloak:8080/realms/DigiTwinStudio"    // Backend Docker perspective
                );
                if (validIssuers.contains(issuer)) {
                    return OAuth2TokenValidatorResult.success();
                }
                OAuth2Error error = new OAuth2Error("invalid_issuer", "The iss claim is not valid", null);
                return OAuth2TokenValidatorResult.failure(error);
            }
        };

        // Combine with timestamp validator
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator
        );

        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }

    /**
     * CORS configuration for local and Docker frontend origins.
     *
     * @return the configured CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",  // Development frontend
                "http://localhost:3000",  // Docker frontend
                "http://frontend:80"      // Internal Docker network
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}