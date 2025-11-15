# Multi-stage build for Time Logging Service
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Stage 2: Runtime image
# Use the jammy JRE image which is multi-arch (supports amd64 and arm64) instead of the alpine variant
# (the alpine variant may not have an arm64 manifest and causes "no match for platform" errors in CI)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a non-root user (Debian/Ubuntu-friendly flags)
RUN addgroup --system spring && adduser --system --ingroup spring --no-create-home spring
USER spring:spring

# Copy the built JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]