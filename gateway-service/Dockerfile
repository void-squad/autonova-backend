# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Auth Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the parent POM first
COPY pom.xml /workspace/parent-pom.xml

# Copy Maven wrapper and auth-service module pom
COPY auth-service/.mvn /workspace/.mvn
COPY auth-service/mvnw /workspace/mvnw
COPY auth-service/pom.xml /workspace/pom.xml

# Pre-download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build the application
COPY auth-service/src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install wget and certificates for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]