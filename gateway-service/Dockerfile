# Multi-stage Dockerfile for building and running the gateway-service Spring Boot app
# Builder stage: uses Maven to compile and package the application
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy the full repository so the reactor build can resolve parent POMs and inter-module deps
COPY . /workspace

# Build only the gateway-service module and its module dependencies (-am = also make required projects)
RUN mvn -f pom.xml -pl gateway-service -am -B -U -DskipTests package

# Runtime stage: use a small Temurin JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for better security and copy the built jar
RUN useradd -m appuser || true
COPY --from=builder /workspace/gateway-service/target/*-SNAPSHOT.jar app.jar
USER appuser

# Expose the default Spring Boot port
EXPOSE 8080

# Allow tuning memory via JAVA_OPTS
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
