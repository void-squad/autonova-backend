# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Discovery Service (Eureka Server)

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# When docker build context is the repository root, copy the discovery service
# files from the repo into the image. Also copy the parent pom.xml from the
# repository root to /pom.xml so the module's parent.relativePath (../pom.xml)
# resolves correctly during the Maven build.

# Copy the parent/root pom so Maven can resolve the aggregator parent
COPY pom.xml /pom.xml

# Copy service-specific mvnw, .mvn and pom
COPY discovery-service/.mvn .mvn
COPY discovery-service/mvnw mvnw
COPY discovery-service/pom.xml pom.xml

# Download dependencies (use wrapper)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code
COPY discovery-service/src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for healthcheck and add CA certs
RUN apk add --no-cache wget ca-certificates

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Eureka server port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
