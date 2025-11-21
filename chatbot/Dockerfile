# syntax=docker/dockerfile:1.6
# Stage 1 - build with maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# copy source
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY src ./src

# Use maven in the container (or mvnw if you added it)
RUN mvn -B -e -DskipTests package

# Stage 2 - runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
  && apt-get install -y --no-install-recommends dumb-init wget ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# create non-root user
RUN groupadd --system spring \
  && useradd --system --gid spring --home /app spring \
  && chown -R spring:spring /app

USER spring:spring
COPY --from=build /workspace/target/*.jar app.jar

ENV PORT=8097
EXPOSE 8097

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -Djava.security.egd=file:/dev/./urandom -jar app.jar"]