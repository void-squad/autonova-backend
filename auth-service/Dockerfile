# Multi-stage Docker build for Auth Service
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the parent/root pom so Maven can resolve the aggregator parent
COPY pom.xml /pom.xml

# Copy service-specific mvnw, .mvn and pom from the repo
COPY auth-service/.mvn .mvn
COPY auth-service/mvnw mvnw
COPY auth-service/pom.xml pom.xml

# Download dependencies using the wrapper (ignore failures so build can continue offline)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code
COPY auth-service/src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install runtime tools and set up non-root user (compatible across platforms)
RUN apt-get update \
    && apt-get install -y --no-install-recommends dumb-init wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 1001 spring \
    && useradd --uid 1001 --gid spring --shell /sbin/nologin spring

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM options tuned for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Entrypoint
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
