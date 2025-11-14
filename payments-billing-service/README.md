# Payments & Billing Service

Minimal API for invoices, Stripe checkout, and billing PDFs.

## Base URL
```
/api
```

## Auth
All endpoints (except health/docs/webhooks) require a **JWT** in `Authorization: Bearer <token>`.
Token must include:
```json
{
  "sub": "user@example.com",           // email
  "userId": 42,                        // number or numeric string
  "role": "CUSTOMER"                   // or EMPLOYEE, ADMIN
}
```
OpenAPI/Swagger: `/v3/api-docs` · `/swagger-ui/index.html`

## Common types

**InvoiceStatus**: `DRAFT | OPEN | PAID | VOID`

**InvoiceResponse**
```json
{
  "id": "2f7d1a39-8a08-4b61-b8d5-8a1f0d6d2f5c",
  "projectId": "adf8b6f3-6c14-4b3d-92e4-0d6e3a5a1c1f",
  "quoteId": "baf3b6a2-1c2d-4e3f-9a8b-1e2d3c4b5a6f",
  "projectName": "On‑site integration",
  "projectDescription": "Install & configure sensors",
  "customerEmail": "user@example.com",
  "customerUserId": 42,
  "currency": "LKR",                   // ISO-4217, 3 letters, uppercase in responses
  "amountTotal": 250000,               // minor units (e.g. cents)
  "paymentMethod": "Stripe",           // or "Offline", may be null before payment
  "status": "OPEN",
  "createdAt": "2025-11-03T12:34:56Z",
  "updatedAt": "2025-11-03T12:34:56Z"
}
```

**Error**
```json
{ "error": "message" }
```

---

## Endpoints

### List invoices
`GET /api/invoices`
- Roles: `CUSTOMER | EMPLOYEE | ADMIN`
- Query:
  - `status` (optional, InvoiceStatus)
  - `projectId` (optional, UUID)
  - `search` (optional, free text against project name/description)
  - `limit` (default 50, min 1, max 100)
  - `offset` (default 0)
- Response `200 OK`:
```json
{
  "items": [ /* InvoiceResponse[] */ ],
  "total": 123,
  "limit": 50,
  "offset": 0
}
```
- Errors: `400` (bad query), `403`

### Get invoice
`GET /api/invoices/{id}`
- Roles: `CUSTOMER | EMPLOYEE | ADMIN`
- Notes: customers can only access their own invoices.
- Response `200 OK`: `InvoiceResponse`
- Errors: `404`, `403`

### Create invoice
`POST /api/invoices`
- Roles: `CUSTOMER`
- Body (`application/json`):
```json
{
  "projectId": "adf8b6f3-6c14-4b3d-92e4-0d6e3a5a1c1f",
  "quoteId": "baf3b6a2-1c2d-4e3f-9a8b-1e2d3c4b5a6f",
  "projectName": "On‑site integration",
  "projectDescription": "Install & configure sensors",
  "amountTotal": 250000,
  "currency": "LKR"
}
```
- Response `201 Created`: `InvoiceResponse`
- Errors: `400` (validation), `409` (invoice already exists for project), `401`/`403`

### Create/reuse Stripe PaymentIntent
`POST /api/invoices/{id}/payment-intent`
- Roles: `CUSTOMER | EMPLOYEE | ADMIN`
- Response `201 Created`:
```json
{
  "paymentIntentId": "pi_abc123",
  "clientSecret": "pi_abc123_secret_…",
  "publishableKey": "pk_test_…"
}
```
- Errors: `400` (invoice not payable), `404`, `502` (Stripe error)

### Mark invoice paid (offline)
`POST /api/invoices/{id}/mark-paid`
- Roles: `EMPLOYEE | ADMIN`
- Response `200 OK`: `InvoiceResponse`
- Errors: `400` (invalid state), `404`, `403`

### Download invoice PDF
`GET /api/invoices/{id}/pdf`
- Roles: `CUSTOMER | EMPLOYEE | ADMIN`
- Response `200 OK`: `application/pdf` (attachment)
- Headers: `X-Invoice-Status`, `X-Invoice-Currency` (if known)
- Errors: `404`, `403`

### Stripe webhook
`POST /webhooks/stripe`
- No auth; **must** include `Stripe-Signature` header.
- Handles event types:
  - `payment_intent.succeeded` → marks invoice `PAID`, emits events
  - `payment_intent.payment_failed` → records failure, emits events
  - `payment_intent.canceled`
- Responses: `200` (processed), `400` (bad/unsigned), `502` (deserialization failure)

---

## Events (RabbitMQ)
Exchanges are configured; routing keys shown below.

- **invoice.created** → exchange: `billing.invoice`
  ```json
  {
    "type": "invoice.created",
    "version": 1,
    "data": {
      "invoice_id": "…",
      "project_id": "…",
      "customer_email": "…",
      "customer_user_id": 42,
      "project_name": "…",
      "project_description": "…",
      "amount_total": 250000,
      "currency": "LKR",
      "status": "OPEN"
    }
  }
  ```
- **invoice.updated** → `billing.invoice` (status changes)
- **payment.succeeded** → `billing.payment` (includes amount/currency/provider)
- **payment.failed** → `billing.payment` (includes error_code)

---

## Status codes
- `200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `502 Bad Gateway`, `500 Internal Server Error`

## Configuration (env)
Required (examples): `PBS_DB_URL`, `PBS_DB_USERNAME`, `PBS_DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PUBLISHABLE_KEY`, `AUTH_JWT_HS256_SECRET`, `PBS_INVOICE_EXCHANGE`, `PBS_PAYMENT_EXCHANGE`, `SERVER_PORT`.

## RabbitMQ – expected setup

**Broker**

* RabbitMQ ≥ 3.10 on a reachable cluster.
* VHost: from `PBS_RABBITMQ_VHOST` (defaults to `/`).
* Auth: `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`.

**Exchanges (declared idempotently by the service on startup)**

* `PBS_INVOICE_EXCHANGE` → default **`billing.invoice`** (type: `topic`, durable: `true`, auto-delete: `false`)
* `PBS_PAYMENT_EXCHANGE` → default **`billing.payment`** (type: `topic`, durable: `true`, auto-delete: `false`)
* Optional DLX `PBS_DLX_EXCHANGE` → default **`billing.dlx`** (type: `topic`, durable: `true`) — used by consumers’ DLQs, not by this service directly.

**Routing keys published**

* `invoice.created`
* `invoice.updated`
* `payment.succeeded`
* `payment.failed`
