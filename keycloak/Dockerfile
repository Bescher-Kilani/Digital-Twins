FROM quay.io/keycloak/keycloak:24.0.2 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database vendor for PostgreSQL
ENV KC_DB=postgres

# Performance optimizations
ENV JAVA_OPTS_APPEND="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m"

WORKDIR /opt/keycloak

# Copy realm configuration for import
COPY keycloak-import/ /opt/keycloak/data/import/

# Build Keycloak with PostgreSQL support
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0.2

# Copy built artifacts from builder stage
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set memory limits
ENV JAVA_OPTS_APPEND="-Xms256m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m"

WORKDIR /opt/keycloak

# Expose Keycloak port
EXPOSE 8080

# Use the Keycloak start script
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# Start in production mode with realm import
# The database configuration will come from environment variables
CMD ["start", \
     "--import-realm", \
     "--http-enabled=true", \
     "--hostname-strict=false", \
     "--hostname-strict-https=false", \
     "--proxy=edge"]