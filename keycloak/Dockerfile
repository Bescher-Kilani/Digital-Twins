FROM quay.io/keycloak/keycloak:24.0.2 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak

# Copy realm configuration
COPY keycloak-import/ /opt/keycloak/data/import/

# Build Keycloak with PostgreSQL support
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0.2

# Copy built artifacts from builder
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set working directory
WORKDIR /opt/keycloak

# Expose port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]