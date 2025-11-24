FROM quay.io/keycloak/keycloak:24.0.2 AS builder

# Copy realm configuration
COPY keycloak-import/ /opt/keycloak/data/import/

# Build optimized Keycloak
WORKDIR /opt/keycloak
RUN /opt/keycloak/bin/kc.sh build

# Production image
FROM quay.io/keycloak/keycloak:24.0.2

# Copy from builder
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Health check script
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD ["/opt/keycloak/bin/kc.sh", "show-config"]

WORKDIR /opt/keycloak

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--optimized", "--http-enabled=true", "--proxy=edge"]