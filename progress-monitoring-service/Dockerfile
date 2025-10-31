## Build stage
FROM maven:3.9.4-eclipse-temurin-21 as builder
WORKDIR /workspace

COPY pom.xml mvnw .mvn/ ./
COPY .mvn .mvn
COPY src ./src

RUN chmod +x mvnw && ./mvnw -DskipTests package -DskipTests

## Run stage 
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# copy built jar (artifact name may vary; use wildcard)
COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/progressmonitoring.jar

# Provide a small entrypoint script to allow JAVA_OPTS and environment overrides
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["/app/entrypoint.sh"]
