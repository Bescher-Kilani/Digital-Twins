FROM quay.io/keycloak/keycloak:24.0.2

# Copy realm configuration
COPY keycloak-import/ /opt/keycloak/data/import/

# Set working directory
WORKDIR /opt/keycloak

# Expose port
EXPOSE 8080

# Production-ready configuration
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--optimized"]