FROM quay.io/keycloak/keycloak:24.0.2 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database vendor
ENV KC_DB=postgres

# Performance optimizations
ENV JAVA_OPTS_APPEND="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m"

WORKDIR /opt/keycloak

# Copy realm configuration
COPY keycloak-import/ /opt/keycloak/data/import/

# Build Keycloak
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0.2

# Copy built artifacts
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set memory limits
ENV JAVA_OPTS_APPEND="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m"

WORKDIR /opt/keycloak

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--http-enabled=true", "--proxy=edge"]