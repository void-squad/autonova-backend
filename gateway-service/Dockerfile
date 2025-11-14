# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Gateway Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy root pom so the module's parent relativePath (../pom.xml) resolves
COPY pom.xml /pom.xml

# Copy module-specific mvnw, .mvn and pom from the repository root context
COPY gateway-service/.mvn .mvn
COPY gateway-service/mvnw mvnw
COPY gateway-service/pom.xml pom.xml

# Download dependencies (allow non-fatal failure to avoid build break on offline images)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code
COPY gateway-service/src ./src

# Build the application with Spring Boot repackaging
RUN ./mvnw clean package spring-boot:repackage -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget and certificates for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the gateway's configured port
EXPOSE 8080

# JVM options (override with Docker env at runtime if needed)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Health check (actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application (allow JAVA_OPTS override)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
