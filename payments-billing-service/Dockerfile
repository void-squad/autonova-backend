# syntax=docker/dockerfile:1.6

ARG JAVA_VERSION=17
ARG MAVEN_VERSION=3.9.7

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS builder

WORKDIR /workspace

# Copy parent POM first to maximize layer caching for dependency resolution
COPY pom.xml ./pom.xml
COPY payments-billing-service/pom.xml ./payments-billing-service/pom.xml

# Copy project sources
COPY payments-billing-service/src ./payments-billing-service/src
COPY payments-billing-service/mvnw ./payments-billing-service/mvnw
COPY payments-billing-service/wrapper ./payments-billing-service/wrapper

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f payments-billing-service/pom.xml -B -DskipTests clean package

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

ARG APP_HOME=/opt/app
ARG APP_USER=appuser

ENV APP_HOME=${APP_HOME} \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

WORKDIR ${APP_HOME}

RUN groupadd --system ${APP_USER} && useradd --system --gid ${APP_USER} ${APP_USER}

COPY --from=builder --chown=${APP_USER}:${APP_USER} /workspace/payments-billing-service/target/*.jar ./app.jar

EXPOSE ${SERVER_PORT}

USER ${APP_USER}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -jar app.jar"]
