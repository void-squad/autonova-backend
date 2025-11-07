# Multi-stage build for Customer Service
# Stage 1: build the application jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy parent pom so the module's relativePath resolves
COPY pom.xml /pom.xml

# Copy Maven wrapper and module pom
COPY customer-service/.mvn .mvn
COPY customer-service/mvnw mvnw
COPY customer-service/pom.xml pom.xml

# Pre-download dependencies (best effort)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code
COPY customer-service/src ./src

# Build the jar (skip tests for faster image build)
RUN ./mvnw clean package -DskipTests

# Stage 2: runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install minimal tooling for health checks and signal handling
RUN apt-get update \
  && apt-get install -y --no-install-recommends dumb-init wget ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# Create non-root user for the app
RUN groupadd --system spring \
  && useradd --system --gid spring spring
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose service port
EXPOSE 8087

# Health check to ensure the service is responsive
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8087/actuator/health || exit 1

# JVM defaults suitable for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
