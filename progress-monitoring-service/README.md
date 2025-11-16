# Progress Monitoring Service

Consumes project-related domain events from RabbitMQ and provides Server-Sent Events (SSE) streams plus a REST API for persisted project messages.

## Features
- Listens to multiple event families: `project.*`, `project.change-request.*`, `quote.*` (and can be extended).
- Derives human-friendly messages from raw JSON payloads.
- Persists messages in PostgreSQL (`project_messages` table) with occurrence and creation timestamps.
- SSE endpoint for live updates: `GET /sse/projects/{projectId}`.
- REST endpoints:
  - `GET /api/projects/{projectId}/messages` (list persisted messages)
  - `POST /api/projects/{projectId}/messages` (create an ad-hoc status / note)

## Configuration
Environment variables (or properties) you can override:

| Property | Env Var | Default | Description |
|----------|---------|---------|-------------|
| `app.rabbit.exchange` | `APP_RABBIT_EXCHANGE` | `autonova.events` | RabbitMQ topic exchange name |
| `app.rabbit.queue` | `APP_RABBIT_QUEUE` | `progress.project.queue` | Queue used by this service |
| `app.rabbit.routing-key` | `APP_RABBIT_ROUTING_KEY` | (empty) | Optional single routing key (legacy mode) |
| `app.rabbit.routing-keys` | `APP_RABBIT_ROUTING_KEYS` | `project.*,quote.*,project.change-request.*` | Comma-separated list of topic patterns |
| `spring.rabbitmq.host` | `SPRING_RABBITMQ_HOST` | `rabbitmq` | RabbitMQ host |
| `spring.rabbitmq.port` | `SPRING_RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `spring.rabbitmq.username` | `SPRING_RABBITMQ_USERNAME` | `guest` | Username |
| `spring.rabbitmq.password` | `SPRING_RABBITMQ_PASSWORD` | `guest` | Password |
| `POSTGRES_HOST` | `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `POSTGRES_DB` | `progress` | DB name |
| `POSTGRES_USER` | `POSTGRES_USER` | `postgres` | DB user |
| `POSTGRES_PASSWORD` | `POSTGRES_PASSWORD` | `postgres` | DB password |

### Multiple vs Single Binding
If `app.rabbit.routing-key` is non-empty it creates a single binding (for backward compatibility). Otherwise it expands `app.rabbit.routing-keys` into multiple bindings.

## SSE Event Names
- `connected` – initial acknowledgement.
- `project.update` – raw JSON payload for any consumed event.
- `project.message` – derived human-friendly message.

## Local Run
```bash
./mvnw spring-boot:run
```
Visit: `http://localhost:8080/sse/projects/{projectId}` with a valid UUID.

## Testing
Unit tests cover:
- Event processor behavior for project, quote and change-request events.
- Rabbit config (single and multiple bindings).
- SSE registry behavior and controller lifecycle.

Run tests:
```bash
./mvnw test
```

## Extending
1. Add a new schema `contracts/events/<topic>.schema.json` with `projectId` in payload.
2. Include its pattern in `app.rabbit.routing-keys`.
3. Optionally enhance `DefaultEventMessageMapper` for custom phrasing.

## Next Steps (Potential Enhancements)
- Add metrics (Micrometer) for events processed / SSE subscribers.
- Dead-letter queue handling & retry policy.
- Filtering SSE stream by category.
- Pagination for `messages` endpoint.


