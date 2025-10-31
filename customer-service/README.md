# Customer Service

Customer Service is a Spring Boot 3 microservice that manages customer profiles and their registered vehicles for the Autonova platform. It exposes REST endpoints for creating, reading, updating, and deleting customer records while enforcing VIN and license-plate uniqueness rules per customer.

## Features

- Customer CRUD with email uniqueness enforcement.
- Vehicle management scoped to a customer with VIN and license plate deduplication.
- PostgreSQL persistence (Neon cloud in production, H2 in tests).
- Service discovery via Eureka (optional locally).
- Consistent REST error payloads via a global exception handler.
- Vehicle lifecycle events emitted via RabbitMQ for downstream subscribers.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Data JPA / Hibernate
- PostgreSQL (production) and H2 (tests)
- Spring Cloud Netflix Eureka client
- Maven Wrapper (`mvnw`)

## Getting Started

### Prerequisites

- Java 21
- Maven (optional; the wrapper is included)
- Access to the configured PostgreSQL instance or a local database

### Environment Configuration

The service reads configuration primarily from `src/main/resources/application.yml` and optionally from a `.env` file located at the project root. Key variables:

| Variable                       | Description                 | Default                                             |
| ------------------------------ | --------------------------- | --------------------------------------------------- |
| `CUSTOMER_SERVICE_DB_URL`      | JDBC URL for PostgreSQL     | `jdbc:postgresql://localhost:5432/customer_service` |
| `CUSTOMER_SERVICE_DB_USERNAME` | Database username           | `customer_service`                                  |
| `CUSTOMER_SERVICE_DB_PASSWORD` | Database password           | `customer_service`                                  |
| `CUSTOMER_SERVICE_PORT`        | HTTP port                   | `8083`                                              |
| `EUREKA_SERVER_URL`            | Eureka discovery endpoint   | `http://localhost:8761/eureka/`                     |
| `RABBITMQ_HOST`                | RabbitMQ host               | `localhost`                                         |
| `RABBITMQ_PORT`                | RabbitMQ port               | `5672`                                              |
| `RABBITMQ_USERNAME`            | RabbitMQ username           | `guest`                                             |
| `RABBITMQ_PASSWORD`            | RabbitMQ password           | `guest`                                             |
| `CUSTOMER_EVENTS_EXCHANGE`     | Exchange for vehicle events | `customer.events`                                   |

### Running Locally

```bash
cd customer-service
./mvnw spring-boot:run
```

> **Note:** If the Eureka server is not running, the service logs connection warnings but remains functional.
>
> To run without Eureka altogether, start the app with the property `eureka.client.enabled=false`, for example:
>
> ```bash
> SPRING_APPLICATION_JSON='{"eureka":{"client":{"enabled":false}}}' ./mvnw spring-boot:run
> ```

### Running Tests

```bash
./mvnw test
```

## REST API

Base URL: `/api`

### Customers

| Method   | Path                      | Description                 |
| -------- | ------------------------- | --------------------------- |
| `POST`   | `/customers`              | Create a customer           |
| `GET`    | `/customers`              | List all customers          |
| `GET`    | `/customers/{customerId}` | Retrieve a customer by id   |
| `PUT`    | `/customers/{customerId}` | Update an existing customer |
| `DELETE` | `/customers/{customerId}` | Delete a customer           |

**Sample Request (Create Customer)**

```http
POST /api/customers
Content-Type: application/json

{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "phone": "+1-555-0100"
}
```

**Sample Response**

```json
{
  "id": 1,
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "phone": "+1-555-0100",
  "vehicles": []
}
```

### Vehicles

All vehicle endpoints are scoped to a customer.

| Method   | Path                                           | Description                       |
| -------- | ---------------------------------------------- | --------------------------------- |
| `POST`   | `/customers/{customerId}/vehicles`             | Register a vehicle for a customer |
| `GET`    | `/customers/{customerId}/vehicles`             | List vehicles for a customer      |
| `GET`    | `/customers/{customerId}/vehicles/{vehicleId}` | Retrieve a vehicle                |
| `PUT`    | `/customers/{customerId}/vehicles/{vehicleId}` | Update a vehicle                  |
| `DELETE` | `/customers/{customerId}/vehicles/{vehicleId}` | Remove a vehicle                  |

**Sample Request (Register Vehicle)**

```http
POST /api/customers/1/vehicles
Content-Type: application/json

{
  "vin": "1HGCM82633A004352",
  "licensePlate": "AXY 2045",
  "make": "Honda",
  "model": "Accord",
  "year": 2023
}
```

**Sample Response**

```json
{
  "id": 5,
  "vin": "1HGCM82633A004352",
  "licensePlate": "AXY 2045",
  "make": "Honda",
  "model": "Accord",
  "year": 2023
}
```

## Error Handling

All failures return a consistent `ApiError` payload:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A vehicle with this VIN already exists",
  "fieldErrors": [],
  "path": "/api/customers/1/vehicles",
  "timestamp": "2025-10-30T17:45:12.156Z"
}
```

Validation failures populate the `fieldErrors` array with field-specific messages, while data-integrity errors rewrite database exceptions into user-friendly messages.

## Event Publishing

After a vehicle is created, updated, or deleted, the service emits a message to RabbitMQ so other microservices can react asynchronously.

- **Exchange:** `customer.events` (configurable via `CUSTOMER_EVENTS_EXCHANGE`)
- **Routing keys:**
  - `vehicle.created`
  - `vehicle.updated`
  - `vehicle.deleted`
- **Payload:** JSON representation of the vehicle event containing event id, type, customer id, vehicle id, VIN, license plate, make, model, year, and an `occurredAt` timestamp (UTC).

Messages are published only after the surrounding database transaction commits successfully, ensuring consumers never observe rolled-back changes.

## Project Structure

```
src/
  main/java/com/autonova/customer/
    controller/        REST layer
    dto/               Request/response models and mappers
    exception/         ApiError record and global handler
    model/             JPA entities
    repository/        Spring Data repositories
    service/           Business rules
  main/resources/      Configuration (`application.yml`)
  test/                Spring Boot integration tests
```

## Additional Notes

- The schema uses uniqueness constraints on VIN globally and on license plates per customer. Conflicts result in HTTP 409 responses.
- The H2 test profile logs a warning about the reserved keyword `year`; functionality is unaffected but can be resolved by renaming the column if needed.
- When running against real services, start the Eureka discovery server before this service to allow successful registration.
