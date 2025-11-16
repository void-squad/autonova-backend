# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Employee Dashboard Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy parent pom to keep the multi-module hierarchy available
COPY pom.xml /pom.xml

# Copy Maven wrapper plus module-level descriptors
COPY employee-dashboard-service/.mvn .mvn
COPY employee-dashboard-service/mvnw mvnw
COPY employee-dashboard-service/pom.xml pom.xml

# Pre-fetch dependencies using the wrapper (best effort)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy module sources and build the jar
COPY employee-dashboard-service/src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health checks (Debian-based)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Environment variables for JVM tuning (can be overridden)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+PrintCommandLineFlags"

# Health check - check readiness endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run application with JVM optimizations
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
