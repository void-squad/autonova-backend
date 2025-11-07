# Project Service

## Local development

```bash
# from autonova-backend/project-service
cd autonova-backend/project-service

# configure environment (Neon connection string, no secrets in source)
cp .env.example .env
# edit .env and set ConnectionStrings__Postgres to your Neon URL

dotnet restore
dotnet build

# add initial EF migrations (first-time setup)
dotnet tool install --global dotnet-ef

# create migration if needed
dotnet ef migrations add InitialCreate

# apply migrations (reads ConnectionStrings__Postgres from environment)
ConnectionStrings__Postgres="Host=your-neon-host.neon.tech;Port=5432;Database=projects_db;Username=project_service;Password=replace-me;Ssl Mode=Require;Trust Server Certificate=false" \
dotnet ef database update

# run service (env variable supplied automatically via .env or shell export)
dotnet run --urls=http://localhost:8082
```

### Idempotent + Change Request endpoints

All mutating endpoints accept the optional `X-Idempotency-Key` header (max 64 chars) and transparently handle retries. New change request surface area:

- `POST /api/projects/{projectId}/change-requests`
- `GET /api/projects/{projectId}/change-requests`
- `GET /api/change-requests/{changeRequestId}`
- `POST /api/change-requests/{changeRequestId}/approve`
- `POST /api/change-requests/{changeRequestId}/reject`
- `POST /api/change-requests/{changeRequestId}/apply`
- `GET /api/projects/{projectId}/status-history`

### Environment variables

Key settings consumed by the service:

- `ConnectionStrings__Postgres` – Neon connection string (required).
- `Auth__Authority` / `Auth__ValidateAudience` – JWT authority configuration.
- `Rabbit__{HostName,UserName,Password,Exchange,Enabled}` – RabbitMQ connection.
- `HealthChecks__CustomersUrl`, `HealthChecks__AppointmentsUrl` – optional downstream readiness probes.
- `Kestrel__Endpoints__Http__Url` – listening URL (defaults to `http://0.0.0.0:8082`).

## Full stack via Docker Compose

```bash
cd autonova-backend/infra
PROJECT_SERVICE_POSTGRES="Host=your-neon-host.neon.tech;Port=5432;Database=projects_db;Username=project_service;Password=replace-me;Ssl Mode=Require;Trust Server Certificate=false" \
docker compose up -d --build
```

Visit:

- Swagger: http://localhost:8082/swagger
- Gateway (proxied): http://localhost:8080/api/projects/...
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- Health: http://localhost:8082/healthz
- Ready: http://localhost:8082/readyz
