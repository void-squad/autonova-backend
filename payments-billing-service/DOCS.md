## 0) Scope (what we will and wonΓÇÖt build)

**We will build**

* One **Payments & Billing** microservice (ΓÇ£PBSΓÇ¥) that:

  * Listens to `quote.approved` events and **creates/updates one invoice per project**.
  * Listens to project completion and **marks the invoice due**.
  * Uses **Stripe Elements** to embed the card form in our UI; PBS creates/reuses **Stripe PaymentIntents** for processing (no raw card data handled by PBS).
  * Processes **Stripe webhooks** to finalize payments.
  * Exposes minimal REST APIs for listing/viewing invoices, starting a checkout, and recording offline payments.
  * Enforces auth using **Project Service roles**.
  * Provides a **downloadable PDF** for any invoice the caller is authorized to view.

**We will not build**

* Refunds / partial refunds.
* Outbox pattern (any distributed TX guarantees beyond ΓÇ£best effortΓÇ¥).
* Eureka / service discovery.
* Advanced accounting (tax, discounts, FX), multiple invoices per project, or complex lineΓÇæitems.

---

## 1) Ground rules & assumptions

1. **IDs**: All serviceΓÇætoΓÇæservice and DB identifiers are **UUID** (Auth will update to UUID; JWT `sub` is a UUID).
2. **Auth/JWT**: Owned by Auth team. PBS only **validates JWT** and reads **roles** (we adopt Project Service roles).
3. **Roles** (effective in JWT): `customer`, `employee`, `manager`.
4. **Events**:

   * **Consume**: `quote.approved` (publisher will be fixed to include needed fields), `project.updated` (to detect completion).
   * **Publish** (minimal): `invoice.created`, `invoice.updated`, `payment.succeeded`, `payment.failed`.
5. **Invoice cardinality**: **One invoice per project**, updated as needed. (Unique by `project_id`.)
6. **Currency**: **LKR** only.
7. **Billing policy**: Create invoice on **approval**, mark as **DUE** when project is **completed**; payment can be made any time, but is required once due.
8. **Payment UI**: **Embedded card form** using Stripe Elements; rely on **webhooks** for truth. PBS never receives raw PAN/CVV.
9. **Messaging**: Use the existing broker (assume RabbitMQ/KafkaΓÇöwire via env vars). **No outbox**; synchronous publish with lightweight retry.
10. **Environment**: Postgres for persistence; containerized deployment.

---

## 2) Architecture (minimal, pragmatic)

* **PBS** (Payments & Billing Service)

  * **Inbound**

    * Message consumer for `quote.approved`, `project.updated`.
    * REST: `/api/invoices`, `/api/invoices/{id}`, `/api/invoices/{id}/payment-intent`, `/api/invoices/{id}/mark-paid`, `/api/invoices/{id}/pdf`.
    * Webhook: `/webhooks/stripe`.
  * **Outbound**

    * Stripe API (PaymentIntents).
    * Event publish: `invoice.*`, `payment.*`.
  * **DB**: Postgres.

* **Service discovery**: **None**. Static service URLs via env variables or Kubernetes service DNS. Gateway optional.

---

## 3) Data model (DDL sketch)

> Use UUIDs, snake_case, and minimal indexing. All timestamps are UTC.

```sql
-- invoices: one row per project
CREATE TABLE invoices (
  id                UUID PRIMARY KEY,
  project_id        UUID NOT NULL UNIQUE,
  customer_id       UUID NOT NULL,
  quote_id          UUID,                    -- optional if provided by event
  currency          CHAR(3) NOT NULL DEFAULT 'LKR',
  amount_total      BIGINT NOT NULL,         -- in minor units (LKR cents)
  status            TEXT NOT NULL,           -- DRAFT | OPEN | DUE | PAID | VOID
  due_at            TIMESTAMPTZ,             -- set when project completes
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_customer ON invoices (customer_id);
CREATE INDEX idx_invoices_status ON invoices (status);

-- payments: each checkout attempt yields at most one row; finalized by webhook
CREATE TABLE payments (
  id                    UUID PRIMARY KEY,
  invoice_id            UUID NOT NULL REFERENCES invoices(id),
  amount                BIGINT NOT NULL,
  currency              CHAR(3) NOT NULL DEFAULT 'LKR',
  provider              TEXT NOT NULL DEFAULT 'STRIPE',
  status                TEXT NOT NULL,  -- INITIATED | SUCCEEDED | FAILED | CANCELED
  stripe_payment_intent_id   TEXT UNIQUE,
  failure_code          TEXT,
  failure_message       TEXT,
  receipt_url           TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_invoice ON payments (invoice_id);

-- (Optional) simple event dedupe for idempotent consumption
CREATE TABLE consumed_events (
  event_id   UUID PRIMARY KEY,
  type       TEXT NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Why minor units?** Stripe uses the smallest currency unit; this spares you floatingΓÇæpoint pain.

---

## 4) States & transitions

### Invoice.status

* `DRAFT` ΓåÆ initial (rarely used; weΓÇÖll jump to `OPEN` directly on approval).
* `OPEN` ΓåÆ created on `quote.approved`.
* `DUE` ΓåÆ when `project.updated` indicates completion.
* `PAID` ΓåÆ when Stripe webhook signals success.
* `VOID` ΓåÆ admin only (not required for MVP; stub allowed).

**Transitions**

* OPEN ΓåÆ DUE (project completed).
* OPEN/DUE ΓåÆ PAID (payment succeeded via Stripe or recorded offline).
* (Admin) OPEN/DUE ΓåÆ VOID.

### Payment.status

* `INITIATED` ΓåÆ when a Stripe **PaymentIntent** is created.
* `SUCCEEDED` ΓåÆ on webhook `payment_intent.succeeded`.
* `FAILED` ΓåÆ on `payment_intent.payment_failed`.
* `CANCELED` ΓåÆ if the PaymentIntent is canceled.

---

## 5) Events (contracts)

### Consumed

#### `quote.approved` (from Project)

```json
{
  "id": "event-uuid",
  "type": "quote.approved",
  "occurred_at": "2025-10-20T10:00:00Z",
  "version": 1,
  "data": {
    "project_id": "uuid",
    "customer_id": "uuid",
    "quote_id": "uuid",
    "total": 1500000,             // LKR in cents
    "currency": "LKR",
    "status": "APPROVED"
  }
}
```

**Handler logic**

* If an invoice for `project_id` **does not exist** ΓåÆ create `OPEN` invoice with `amount_total = total`.
* If it **exists** and is not `PAID`/`VOID` ΓåÆ update `amount_total` (idempotent: ignore duplicate events via `consumed_events`).

#### `project.updated` (from Project)

```json
{
  "id": "event-uuid",
  "type": "project.updated",
  "occurred_at": "2025-10-21T12:00:00Z",
  "version": 1,
  "data": {
    "project_id": "uuid",
    "status": "Completed"         // case-sensitive per publisher
  }
}
```

**Handler logic**

* If invoice exists and `status` is `OPEN`, set `status = DUE` and `due_at = now()`.

### Published (minimal)

#### `invoice.created`

```json
{ "type":"invoice.created", "version":1, "data": { "invoice_id":"uuid", "project_id":"uuid", "customer_id":"uuid", "amount_total":1500000, "currency":"LKR", "status":"OPEN" } }
```

#### `invoice.updated`

```json
{ "type":"invoice.updated", "version":1, "data": { "invoice_id":"uuid", "status":"DUE" } }
```

#### `payment.succeeded`

```json
{ "type":"payment.succeeded", "version":1, "data": { "payment_id":"uuid", "invoice_id":"uuid", "project_id":"uuid", "amount":1500000, "currency":"LKR", "provider":"STRIPE" } }
```

#### `payment.failed`

```json
{ "type":"payment.failed", "version":1, "data": { "payment_id":"uuid", "invoice_id":"uuid", "error_code":"card_declined" } }
```

> **Publishing simplicity**: publish **after DB commit**; on publish failure, log and enqueue a **bestΓÇæeffort retry** (e.g., transient inΓÇæmemory/backoff worker). ThatΓÇÖs itΓÇöno outbox.

---

## 6) REST API (minimal)

Base URL: `/api` (behind gateway if present)

### Invoices

**GET** `/api/invoices?status=OPEN|DUE|PAID&projectId=uuid&limit=50&offset=0`

* **Auth**:

  * `customer`: only their own invoices (`customer_id == sub`).
  * `employee|manager`: all invoices.
* **200**:

```json
{ "items": [ { "id":"uuid","project_id":"uuid","customer_id":"uuid","amount_total":1500000,"currency":"LKR","status":"OPEN","due_at":null,"created_at":"..."} ],
  "total": 1, "limit": 50, "offset": 0 }
```

**GET** `/api/invoices/{id}`

* Same auth rules as above.
* **200**: invoice object.

**POST** `/api/invoices/{id}/payment-intent`

* Purpose: create or reuse a **Stripe PaymentIntent** for this invoice to enable embedded card payments via Stripe Elements.
* **Auth**:

  * `customer`: only if owner and invoice not `PAID`.
  * `employee|manager`: allowed (for assisting).
* **Body** (optional):

```json
{}
```

* **201**:

```json
{ "client_secret": "pi_123_secret_abc" }
```

* **Behavior**:

  * If a prior **active** PaymentIntent exists for the invoice and it is not succeeded/canceled, return its `client_secret`.
  * Otherwise, create a new PaymentIntent (`amount`, `currency`, `metadata={invoiceId, projectId, customerId}`), persist a `payments` row with `status=INITIATED` and `stripe_payment_intent_id`, and return its `client_secret`.

**POST** `/api/invoices/{id}/mark-paid`

* Purpose: allow an employee/manager to record an **offline payment** (bank transfer, POS, etc.) without card processing.
* **Auth**: `employee|manager` only.
* **Body** (optional):

```json
{}
```

* **200**:

```json
{ "id":"uuid","project_id":"uuid","customer_id":"uuid","amount_total":1500000,"currency":"LKR","status":"PAID","due_at":null,"created_at":"..." }
```

* **Behavior**:

  * Rejects invoices that are `PAID`, `VOID`, or still `DRAFT`.
  * Persists a `payments` row with `provider=OFFLINE`, `status=SUCCEEDED`, and the invoice amount.
  * Marks the invoice `PAID` and publishes `invoice.updated` and `payment.succeeded`.
* **Notes**: assumes funds cover the full invoice amount; partial payments remain unsupported.

**GET** `/api/invoices/{id}/pdf`

* Purpose: download the invoice as a **PDF** suitable for printing/sharing.
* **Auth**:

  * `customer`: only if owner (`customer_id == sub`).
  * `employee|manager`: allowed.
* **200**: returns PDF bytes with headers:

  * `Content-Type: application/pdf`
  * `Content-Disposition: attachment; filename="invoice-{id}.pdf"`
* **404** if invoice not found or caller not authorized.
* **Notes**:

  * PDF is rendered from an HTML template (company branding, line items, totals) using the chosen library (see section 17).
  * The endpoint is idempotent and safe to cache for a short TTL (e.g., 5 minutes) if invoice is not `PAID`/`VOID` and content is deterministic.
  * Include invoice metadata in response headers if helpful (e.g., `X-Invoice-Status`, `X-Invoice-Currency`).

### Stripe Webhook

**POST** `/webhooks/stripe`

* Verify **Stripe signature** header.
* Handle:

  * `payment_intent.succeeded` ΓåÆ mark payment `SUCCEEDED` and invoice `PAID`, persist `receipt_url` from the latest charge if available, publish `payment.succeeded`.
  * `payment_intent.payment_failed` ΓåÆ mark `FAILED`, store `failure_code/message`, publish `payment.failed`.
  * (Optional) `payment_intent.canceled` ΓåÆ mark `CANCELED`.
* **Idempotency**: enforce **unique** `stripe_payment_intent_id`.

---

## 7) Security & authorization

* **JWT** validated using AuthΓÇÖs configuration (HS256 or OIDCΓÇöowned by Auth team).
* Roles used by PBS: `customer`, `employee`, `manager`.
* **Access rules**

  * `GET /api/invoices` and `GET /api/invoices/{id}`:

    * `customer` ΓåÆ filter to `customer_id == sub`.
    * `employee|manager` ΓåÆ full access.
  * `POST /api/invoices/{id}/payment-intent`:

    * `customer` (owner) or `employee|manager`.
  * Webhook is unauthenticated but **must** verify Stripe signature.
  * `GET /api/invoices/{id}/pdf` follows the same authorization rules as `GET /api/invoices/{id}`.

* **PCI & data handling**

  * Only use Stripe Elements on the frontend to collect card details; PBS never handles raw card data.
  * Store and log only non-sensitive identifiers (e.g., `payment_intent_id`); avoid logging tokens or full webhook bodies containing PII.

---

## 8) Idempotency & consistency (without outbox)

* **Event consumption**: record `event_id` in `consumed_events`. If seen, **noΓÇæop**.
* **Invoice creation**: `UNIQUE(project_id)` guarantees **one invoice per project**. Use UPSERT on `quote.approved`.
* **PaymentIntent**: if an unpaid invoice already has an active PaymentIntent, **reuse** it.
* **Webhook**: dedupe by **unique** `stripe_payment_intent_id`.
* **Publishing**: after transactions, publish events. On failure, retry a few times in process; persist an error log entry. (Good enough for the assignment.)

---

## 9) Configuration (env vars)

```
# Database
PBS_DB_URL=postgres://user:pass@host:5432/payments

# Auth (owned by Auth team; PBS just consumes)
AUTH_JWT_ISSUER=...
AUTH_JWT_AUDIENCE=...
AUTH_JWT_JWKS_URL=...      # or AUTH_JWT_HS256_SECRET=...

# Messaging
MSG_BROKER_HOST=...
MSG_BROKER_EXCHANGE=platform.events
MSG_QUEUE_QUOTE_APPROVED=quote.approved.pbs
MSG_QUEUE_PROJECT_UPDATED=project.updated.pbs

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PUBLISHABLE_KEY=pk_test_...   # used by the frontend for Stripe Elements

# Service
PORT=8080
```

> Note: If Stripe test mode struggles with LKR, fall back to a test currency **in dev only** (e.g., USD). Prod remains LKR.

---

## 10) HappyΓÇæpath flows (step by step)

**A) Quote approved ΓåÆ invoice created (OPEN)**

1. Project publishes `quote.approved`.
2. PBS consumes ΓåÆ UPSERT invoice `(project_id, customer_id, amount_total, currency)`, `status=OPEN`.
3. PBS publishes `invoice.created`.

**B) Customer pays in-app (Stripe Elements)**

1. Client calls `POST /api/invoices/{id}/payment-intent` to get a `client_secret`.
2. Frontend renders Stripe Elements, collects card details, and calls `stripe.confirmCardPayment(client_secret, {payment_method: {card, billing_details}})`.
3. On success, the UI shows a confirmation; source of truth remains the webhook.
4. **Webhook** `payment_intent.succeeded` ΓåÆ PBS sets `payments.status=SUCCEEDED`, `invoices.status=PAID`, stores `payment_intent_id`, `receipt_url`, publishes `payment.succeeded`.

**D) User downloads invoice PDF**

1. Client calls `GET /api/invoices/{id}/pdf`.
2. PBS authorizes the caller using the same rules as `GET /api/invoices/{id}`.
3. PBS renders HTML ΓåÆ PDF using the chosen library and streams bytes with `Content-Type: application/pdf`.

**C) Project completed ΓåÆ invoice becomes DUE**

1. Project publishes `project.updated` with `status=Completed`.
2. PBS sets invoice to `DUE` (unless already `PAID`) and `due_at=now()`.
3. PBS publishes `invoice.updated` with new status.

---

## 11) Minimal implementation order (do this in sequence)

1. **DB migrations** for `invoices`, `payments`, `consumed_events`.
2. **Event consumer** for `quote.approved` (UPSERT invoice).
3. **GET invoices** endpoints with auth filters.
4. **Stripe PaymentIntents** integration + `POST /invoices/{id}/payment-intent`.
5. **Webhook** endpoint with signature verification + status updates.
6. **Event consumer** for `project.updated` ΓåÆ mark invoice `DUE`.
7. **Publish** events (`invoice.created`, `invoice.updated`, `payment.succeeded`, `payment.failed`).
8. **Basic retries/logging** for publish failures.
9. **Tests** (see next).
10. **Invoice PDF**: HTML template + PDF renderer; secure `GET /invoices/{id}/pdf`.

---

## 12) Testing plan (lean but covers risk)

**Unit**

* Invoice UPSERT logic on `quote.approved`.
* Role filtering on GET endpoints.
* Mapping of webhook events ΓåÆ payment/invoice states.

**Integration (Testcontainers)**

* Postgres: migrations, unique constraints, idempotency of `consumed_events`.
* Messaging: consume sample `quote.approved` / `project.updated` payloads.
* Stripe: mock webhook requests with valid signature (use StripeΓÇÖs test helper or library util).

**Contract**

* JSON schema validation for both **consumed** and **published** events (shape + required fields).

**Manual**

* Full happy path in test mode: approve quote ΓåÆ list invoice (OPEN) ΓåÆ checkout ΓåÆ webhook ΓåÆ invoice (PAID).
* Completion path: complete project ΓåÆ invoice flips to DUE if unpaid.
* PDF: download as authorized customer and employee; verify headers, filename, and that content renders totals and IDs correctly.

---

## 13) API request/response examples

**Create/Retrieve PaymentIntent**

```
POST /api/invoices/5cdd0b7e-.../payment-intent
Authorization: Bearer <jwt>

201
{ "client_secret": "pi_123_secret_abc" }
```

**Stripe Webhook (success path)**

* Handle `payment_intent.succeeded`:

```json
{
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_123",
      "metadata": { "invoiceId": "uuid", "projectId": "uuid", "customerId": "uuid" },
      "charges": { "data": [ { "receipt_url": "https://..." } ] }
    }
  }
}
```

* PBS: set `payments.status=SUCCEEDED`, `invoices.status=PAID`, publish `payment.succeeded`.

---

## 14) Observability & ops (basic)

* **Log**: event IDs, invoice IDs, payment intent IDs, and publish outcomes.
* **Metrics** (optional): invoices by status, webhook success/failure count, publish retry count.
* **Health**: `/health` reports DB + broker connectivity.

---

## 15) Risks & how weΓÇÖre accepting them (since this is a module project)

* **No outbox** ΓåÆ small chance of ΓÇ£DB wrote, publish failedΓÇ¥. WeΓÇÖll accept it; log + bestΓÇæeffort retry.
* **One invoice per project** ΓåÆ no deposits/change orders. *By design.*
* **Stripe currency/test mode** ΓåÆ if LKR isnΓÇÖt supported in your test account, dev uses USD; prod keeps LKR.

---

## 16) Definition of Done (for this module)

* Migrations applied; service starts; health passes.
* Consumes `quote.approved` and creates `OPEN` invoice.
* Consumes project completion and marks invoice `DUE`.
* `GET /api/invoices*` works with roleΓÇæbased filtering.
* `POST /api/invoices/{id}/payment-intent` returns a **Stripe client_secret** for the frontend to confirm via Stripe Elements.
* Webhook drives invoice to `PAID` and records payment.
* Publishes `invoice.created|updated` and `payment.succeeded|failed`.
* Tests: unit + minimal integration + a couple of contract checks.
* Invoice can be downloaded as **PDF** via `GET /api/invoices/{id}/pdf` with correct auth and headers.

---

## 17) Invoice PDFs (library choice & approach)

**Library**: OpenHTMLtoPDF (HTML-to-PDF renderer for Java)

**Rendering approach**

1. Build an invoice HTML using a server-side template (e.g., Thymeleaf/Freemarker) with company logo, project/invoice IDs, line items, totals, currency, and status badge.
2. Feed the HTML string (and optional base URL for assets) into OpenHTMLtoPDF and stream the resulting bytes.
3. Set headers: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="invoice-{id}.pdf"`.
4. Ensure fonts are embedded for consistent rendering (particularly for currency symbols); store fonts under `src/main/resources/fonts`.
