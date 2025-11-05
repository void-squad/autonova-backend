FROM maven:3.9.3-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy the entire repository so we can build the multi-module project and the discovery module
COPY . /workspace

# Build only the discovery-service and its module dependencies
RUN mvn -f pom.xml -pl discovery-service -am -DskipTests package

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
