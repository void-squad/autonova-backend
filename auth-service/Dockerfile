# syntax=docker/dockerfile:1.6
# Multi-stage Docker build for Auth Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy root pom so the module's parent relativePath (../pom.xml) resolves
COPY pom.xml /pom.xml

# Copy service-specific mvnw, .mvn and pom from the repository root context
COPY auth-service/.mvn .mvn
COPY auth-service/mvnw mvnw
COPY auth-service/pom.xml pom.xml

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code
COPY auth-service/src ./src

# Build the application with Spring Boot repackaging
RUN ./mvnw clean package spring-boot:repackage -DskipTests

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

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]