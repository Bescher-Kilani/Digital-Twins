package org.DigiTwinStudio.DigiTwin_Backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class to conditionally enable scheduled tasks.
 *
 * Scheduling is enabled by default (matchIfMissing = true) for local development,
 * but can be disabled in production environments like Railway by setting
 * the environment variable SCHEDULING_ENABLED=false.
 */
@Configuration
@ConditionalOnProperty(
        name = "scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true  // Enabled by default if property is not set
)
@EnableScheduling
public class SchedulingConfig {
    // This class enables scheduling only when the property is true or missing
}