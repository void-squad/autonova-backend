# Autonova Backend - Setup Guide

## Overview

This repository contains the backend microservices for the Autonova project (Java + .NET services). We use Neon (managed Postgres) for all databases and provide `infra/docker-compose.yml` to run the microservices and RabbitMQ locally.

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


   This will build the local services and start RabbitMQ (Postgres lives in Neon). Services and ports:
   - gateway-service: 8080
   - project-service: 8082
   - auth-service: 8082
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

### 2. Wait for services to start. RabbitMQ has a healthcheck; Neon provisioning is handled by the helper container described below.

Run services locally (without Docker)

- Java services (example):
  cd discovery-service
  mvn spring-boot:run

  For other Maven services replace directory and run `mvn spring-boot:run` or `mvn -DskipTests package` and run the artifact.

- project-service (.NET):
  cd project-service
  dotnet restore
  dotnet build
  dotnet run --urls "http://localhost:8082"

### 3. Configure Neon database access

  - Copy `infra/.env.example` to `infra/.env` and update the Neon host/user/password secrets.
  - The `postgres-init` helper container uses those credentials to provision per-service databases and roles in Neon. It does **not** start a local Postgres instance.
  - Example connection string values:
    - `PROJECT_SERVICE_POSTGRES` — `Host=your-neon-host.neon.tech;Port=5432;Database=projects_db;Username=project_service;Password=<secret>;Ssl Mode=Require;Trust Server Certificate=false`
    - `AUTH_SERVICE_POSTGRES` — Neon connection string consumed by `auth-service`
