# syntax=docker/dockerfile:1.6
# Multi-stage build for Time Logging Service aligned with repository root context

# Stage 1: Build the application using Maven wrapper
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Provide parent pom for multi-module resolution
COPY pom.xml /pom.xml

# Copy module-specific Maven wrapper and pom
COPY time-logging-service/.mvn .mvn
COPY time-logging-service/mvnw mvnw
COPY time-logging-service/pom.xml pom.xml

# Warm up Maven cache (best effort) to speed up subsequent builds
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build JAR
COPY time-logging-service/src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install wget for healthchecks and create non-root user
RUN apt-get update \
  && apt-get install -y --no-install-recommends wget \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact
COPY --from=build /workspace/target/*.jar app.jar

# Expose application port
EXPOSE 8083

# Health check leveraging actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Allow JVM flags to be overridden via JAVA_OPTS
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Launch the service
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]