# Multi-stage build für Spring Boot Backend
# Build Stage
FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven files FROM ROOT
COPY Backend/pom.xml .
COPY Backend/src ./src

# Download dependencies
RUN mvn dependency:go-offline -B

# Build application
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]