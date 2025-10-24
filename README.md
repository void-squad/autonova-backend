# Autonova Backend - Setup Guide

## Overview

This repository contains the backend microservices for the Autonova project (Java + .NET services) and an `infra/docker-compose.yml` to start a local development environment with Postgres and RabbitMQ.

## Prerequisites

- Docker & Docker Compose
- .NET SDK
- Java
- Maven

## Quick start

### 1. From the repository root run:

  #### To build and start services with Docker Compose:

  ```bash
  docker compose -f infra/docker-compose.yml up --build
  ```

  or navigate to the `infra` directory and run:
  
  ```bash
  cd infra
  docker compose up --build
  ```


   This will build the local services and start Postgres and RabbitMQ. Services and ports:
   - gateway-service: 8080
   - project-service: 8081
   - auth-service: 8082
   - Postgres: 5432
   - RabbitMQ management: 15672

   #### Or if you have built the images before:

  ```bash
  docker compose -f infra/docker-compose.yml up
  ```

  or 
  
  ```bash
  cd infra
  docker compose up
  ```   

### 2. Wait for services to start. Postgres has a healthcheck; migration steps (if needed) are described below.

Run services locally (without Docker)

- Java services (example):
  cd discovery-service
  mvn spring-boot:run

  For other Maven services replace directory and run `mvn spring-boot:run` or `mvn -DskipTests package` and run the artifact.

- project-service (.NET):
  cd services/project-service
  dotnet restore
  dotnet build
  dotnet run --urls "http://localhost:8081"

### 3. Creating databases for the microservices

  - The compose file includes a postgres-init service that runs infra/init-db/init-db.sh to create the required databases and users.
  - Edit the SERVICES array in that script to add or change databases/users. Example:
  ```bash
  SERVICES=(
    "projects_db:project_service:PROJECTS_DB_PASSWORD"
    "progress_monitoring_db:progress_monitoring_service:PROGRESS_MONITORING_DB_PASSWORD"
  )
  ```
  - Environment variables are read from infra/.env — copy infra/.env.example to infra/.env if missing.

  - `PROJECT_SERVICE_POSTGRES` — e.g. `Host=postgres;Database=project_db;Username=project_user;Password=project_pass`
  - `AUTH_SERVICE_POSTGRES` — connection string used by `auth-service`
