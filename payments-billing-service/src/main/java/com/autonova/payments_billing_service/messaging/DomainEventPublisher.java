package com.autonova.payments_billing_service.messaging;

import com.autonova.payments_billing_service.config.MessagingProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;
    private final ObjectMapper objectMapper;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties properties, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publishInvoiceCreated(InvoiceEntity invoice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "invoice.created");
        payload.put("version", 1);
        payload.put("data", Map.of(
            "invoice_id", invoice.getId(),
            "project_id", invoice.getProjectId(),
            "customer_id", invoice.getCustomerId(),
            "amount_total", invoice.getAmountTotal(),
            "currency", invoice.getCurrency(),
            "status", invoice.getStatus().name()
        ));

        send(properties.getOutbound().getInvoiceExchange(), "invoice.created", payload);
    }

    public void publishInvoiceUpdated(InvoiceEntity invoice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "invoice.updated");
        payload.put("version", 1);
        payload.put("data", Map.of(
            "invoice_id", invoice.getId(),
            "status", invoice.getStatus().name()
        ));

        send(properties.getOutbound().getInvoiceExchange(), "invoice.updated", payload);
    }

    public void publishPaymentSucceeded(InvoiceEntity invoice, PaymentEntity payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment.succeeded");
        payload.put("version", 1);
        payload.put("data", Map.of(
            "payment_id", payment.getId(),
            "invoice_id", invoice.getId(),
            "project_id", invoice.getProjectId(),
            "amount", payment.getAmount(),
            "currency", payment.getCurrency()
        ));

        send(properties.getOutbound().getPaymentExchange(), "payment.succeeded", payload);
    }

    public void publishPaymentFailed(UUID paymentId, UUID invoiceId, String failureCode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment.failed");
        payload.put("version", 1);
        payload.put("data", Map.of(
            "payment_id", paymentId,
            "invoice_id", invoiceId,
            "error_code", failureCode
        ));

        send(properties.getOutbound().getPaymentExchange(), "payment.failed", payload);
    }

    private void send(String exchange, String routingKey, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(exchange, routingKey, body);
            log.debug("Published event {} to exchange {}", payload.get("type"), exchange);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize event payload", e);
        }
    }
}
