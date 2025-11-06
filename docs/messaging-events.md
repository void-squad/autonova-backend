AUTONOVA Messaging & Events (RabbitMQ → Notification Service)

Purpose
- Standardize how services publish domain events so notification-service can consume and fan out via SSE.

Exchange
- Name: autonova.events
- Type: topic
- Durable: true
- Content type: application/json
- Delivery mode: 2 (persistent)

Routing keys
- Use the same string as the event “type” field in payload, lowercase with dots.
- Examples already in contracts: invoice.created, invoice.updated, payment.failed, payment.succeeded, project.created, project.updated, quote.approved, quote.rejected, user.logged-in, vehicle.event
- New you can add: appointment.created, appointment.accepted, appointment.rejected, appointment.rescheduled, appointment.cancelled, appointment.in_progress, appointment.completed, project.requested, project.approved, project.in_progress, project.completed, project.cancelled, time_log.created, time_log.approved, time_log.rejected

Message properties (AMQP)
- message_id: UUID (required)
- timestamp: set by publisher (recommended)
- app_id: service name (recommended)
- correlation_id: to link to HTTP request/trace (recommended)
- content_type: application/json
- headers (recommended, do not break schemas):
  - x-event-name: same as routing key (e.g., appointment.created)
  - x-event-version: integer version, default 1
  - x-actor-id: optional (who triggered)
  - x-actor-role: CUSTOMER|EMPLOYEE|ADMIN (optional)
  - x-recipients-user-ids: CSV of user IDs to notify (optional)
  - x-recipients-roles: CSV of roles to notify (optional)
  - x-tenant-id, x-trace-id: optional

Payload contract (aligns with existing contracts/*)
- Keep top-level strict to match JSON Schemas under contracts/events/.
- Required top-level fields:
  - type: string, equals routing key (e.g., "payment.succeeded")
  - version: integer, e.g., 1
  - data: object with event-specific fields (snake_case recommended)
- Do NOT add extra top-level fields unless you update the schema; use AMQP headers for meta/recipients.

Examples (payloads)
- appointment.created (proposed)
  {
    "type": "appointment.created",
    "version": 1,
    "data": {
      "appointment_id": "<uuid>",
      "customer_id": "<uuid>",
      "vehicle_id": "<uuid>",
      "service_type": "MAINTENANCE|REPAIR|DIAGNOSTIC|DETAILING",
      "scheduled_at": "2025-11-08T10:00:00Z",
      "status": "PENDING",
      "notes": "optional"
    }
  }
- appointment.accepted (proposed)
  {
    "type": "appointment.accepted",
    "version": 1,
    "data": {
      "appointment_id": "<uuid>",
      "previous_status": "PENDING",
      "status": "ACCEPTED",
      "assigned_employee_id": "<uuid>"
    }
  }
- payment.succeeded (existing style)
  // See contracts/events/payment.succeeded.schema.json for the exact fields
  {
    "type": "payment.succeeded",
    "version": 1,
    "data": {
      "payment_id": "<uuid>",
      "invoice_id": "<uuid>",
      "project_id": "<uuid>",
      "amount": 19900,
      "currency": "USD",
      "provider": "STRIPE"
    }
  }

Notification-service bindings
- Queue: notification-service.events (durable)
- Bindings (topic):
  - appointment.*
  - project.*
  - time_log.*
  - payment.*
  - invoice.*
  - quote.*
  - user.logged-in
  - vehicle.event
- Optional DLQ: autonova.dlx → notification-service.events.dlq

Publisher rules
- Use publisher confirms; treat success only after ack.
- Publish persistent messages; include message_id.
- Never introduce breaking changes within the same version; bump version when needed and update schema.
- Keep payload PII-minimal.

How to extend contracts
- Add JSON Schemas in contracts/events/ matching the payloads above, following existing patterns (see payment.succeeded.schema.json).
- Use $id like: "autonova://events/appointment.created.v1" and enforce snake_case fields in data.

Minimal publisher checklist
- Exchange: autonova.events (topic)
- Routing key: equals payload.type
- Properties: message_id (UUID), content_type application/json, delivery_mode 2
- Headers: x-event-name, x-event-version, optional x-recipients-* for notification targeting
- Payload: { type, version, data }

Code examples
- Java (Spring Boot + spring-amqp)
  @Service
  public class EventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public EventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    public void publishAppointmentCreated(AppointmentCreated evt) {
      MessageProperties props = new MessageProperties();
      props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
      props.setMessageId(UUID.randomUUID().toString());
      props.setTimestamp(new Date());
      props.setHeader("x-event-name", "appointment.created");
      props.setHeader("x-event-version", 1);
      props.setHeader("x-recipients-user-ids", String.join(",", List.of(evt.getCustomerId())));
      Message msg = new Message(new ObjectMapper().writeValueAsBytes(evt), props);
      rabbitTemplate.send("autonova.events", "appointment.created", msg);
    }
  }
  // Where AppointmentCreated has fields: type="appointment.created", version=1, data={...}

- Node.js (amqplib)
  const amqp = require('amqplib');
  async function publish(ch, payload) {
    const exchange = 'autonova.events';
    const routingKey = payload.type;
    const ok = ch.publish(exchange, routingKey, Buffer.from(JSON.stringify(payload)), {
      contentType: 'application/json',
      deliveryMode: 2,
      messageId: crypto.randomUUID(),
      timestamp: Date.now(),
      headers: {
        'x-event-name': payload.type,
        'x-event-version': payload.version,
        'x-recipients-user-ids': payload.data.customer_id ?? ''
      }
    });
    if (!ok) await new Promise(r => ch.once('drain', r));
  }

- .NET (RabbitMQ.Client)
  var factory = new ConnectionFactory { Uri = new Uri(rmqUrl) };
  using var conn = factory.CreateConnection();
  using var ch = conn.CreateModel();
  var props = ch.CreateBasicProperties();
  props.ContentType = "application/json";
  props.DeliveryMode = 2;
  props.MessageId = Guid.NewGuid().ToString();
  props.Timestamp = new AmqpTimestamp(DateTimeOffset.UtcNow.ToUnixTimeSeconds());
  props.Headers = new Dictionary<string, object> {
    ["x-event-name"] = "appointment.created",
    ["x-event-version"] = 1,
    ["x-recipients-user-ids"] = customerId
  };
  var payload = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new { type = "appointment.created", version = 1, data = new { /* ... */ } }));
  ch.BasicPublish("autonova.events", "appointment.created", props, payload);

Notes
- Keep routing keys and payload.type identical.
- For broadcasts to all employees/admins, prefer x-recipients-roles over enumerating user IDs.
- For multi-tenant, include x-tenant-id header and put tenant_id in data when relevant.
