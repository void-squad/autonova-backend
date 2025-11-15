# syntax=docker/dockerfile:1.6
## ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -q dependency:go-offline -B
COPY src ./src
RUN mvn -q -DskipTests package

## ---------- Run stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV JAVA_OPTS="-Xms256m -Xmx512m"
COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/notification.jar
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/notification.jar"]
