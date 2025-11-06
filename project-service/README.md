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
dotnet run --urls=http://localhost:8081
```

## Full stack via Docker Compose

```bash
cd autonova-backend/infra
PROJECT_SERVICE_POSTGRES="Host=your-neon-host.neon.tech;Port=5432;Database=projects_db;Username=project_service;Password=replace-me;Ssl Mode=Require;Trust Server Certificate=false" \
docker compose up -d --build
```

Visit:

- Swagger: http://localhost:8081/swagger
- Gateway (proxied): http://localhost:8080/api/projects/...
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- Health: http://localhost:8081/healthz
