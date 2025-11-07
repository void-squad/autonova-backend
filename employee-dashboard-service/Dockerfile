# Multi-stage Docker build for Employee Dashboard Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy parent pom so the module's relativePath resolves
COPY pom.xml /pom.xml

# Copy service pom
COPY employee-dashboard-service/pom.xml pom.xml

# Download dependencies (best effort)
RUN mvn dependency:go-offline -B || true

# Copy source code and build
COPY employee-dashboard-service/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
