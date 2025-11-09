# syntax=docker/dockerfile:1.4
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy parent POM and module sources (context is repo root)
COPY pom.xml ./pom.xml
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY gateway-service/src ./gateway-service/src

# download dependencies and build the module
RUN mvn -f gateway-service/pom.xml -B -DskipTests dependency:go-offline
RUN mvn -f gateway-service/pom.xml -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring --home /app spring || true
COPY --from=build /workspace/gateway-service/target/*.jar app.jar
RUN chown spring:spring /app/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]