# Multi-stage build for Analytics Service
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy only the analytics-service directory
COPY analytics-service/pom.xml ./analytics-service/
COPY analytics-service/src ./analytics-service/src

# Build the service
WORKDIR /build/analytics-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Install curl for the container-level health check
RUN apt-get update \
  && apt-get install --no-install-recommends -y curl \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/analytics-service/target/analytics-service.jar app.jar

# Expose port 8089
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f --silent http://localhost:8089/api/analytics/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]