# Project Service

## Local development

```bash
# from autonova-backend/services/project-service-dotnet/ProjectService
cd autonova-backend/services/project-service-dotnet/ProjectService

dotnet restore
dotnet build

# add initial EF migrations (create one DbContext + Migrations folder)
dotnet tool install --global dotnet-ef
dotnet ef migrations add InitialCreate
dotnet ef database update

# run service
dotnet run --urls=http://localhost:8081
```

## Full stack via Docker Compose

```bash
cd autonova-backend/infra
docker compose up -d --build
```

Visit:

- Swagger: http://localhost:8081/swagger
- Gateway (proxied): http://localhost:8080/api/projects/...
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- Health: http://localhost:8081/healthz
