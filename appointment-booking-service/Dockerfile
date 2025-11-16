# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Appointment Booking Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Keep the parent pom so Maven can resolve the multi-module structure
COPY pom.xml /pom.xml

# Copy Maven wrapper and module descriptor
COPY appointment-booking-service/.mvn .mvn
COPY appointment-booking-service/mvnw mvnw
COPY appointment-booking-service/pom.xml pom.xml

# Prime the Maven cache (best effort)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy sources and build
COPY appointment-booking-service/src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health checks (Debian-based)
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

# Run as non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the jar built in the previous stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose service port
EXPOSE 8088

# JVM defaults (overridable)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Health check using the actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

# Launch the Spring Boot app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
