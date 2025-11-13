# syntax=docker/dockerfile:1.6
## ---------- Build stage ----------
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src

RUN ./mvnw -DskipTests package


## ---------- Run stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/progressmonitoring.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh && useradd -m appuser
USER appuser

EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["./entrypoint.sh"]
