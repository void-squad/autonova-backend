package com.autonova.payments_billing_service.messaging;

import com.autonova.payments_billing_service.config.MessagingProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
    private static final int MAX_PUBLISH_ATTEMPTS = 3;
    private static final long BASE_RETRY_BACKOFF_MS = 100L;

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
            "currency", formatCurrency(invoice.getCurrency()),
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
            "currency", formatCurrency(payment.getCurrency()),
            "provider", payment.getProvider().name()
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

    private String formatCurrency(String currency) {
        return currency == null ? null : currency.toUpperCase(Locale.ROOT);
    }

    private void send(String exchange, String routingKey, Map<String, Object> payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize event payload", e);
        }

        AmqpException lastException = null;
        for (int attempt = 1; attempt <= MAX_PUBLISH_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, body);
                log.debug("Published event {} to exchange {}", payload.get("type"), exchange);
                return;
            } catch (AmqpException ex) {
                lastException = ex;
                log.warn(
                    "Attempt {}/{} to publish event {} failed: {}",
                    attempt,
                    MAX_PUBLISH_ATTEMPTS,
                    payload.get("type"),
                    ex.getMessage()
                );
                if (attempt < MAX_PUBLISH_ATTEMPTS) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt - 1) * BASE_RETRY_BACKOFF_MS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new IllegalStateException("Failed to publish event " + payload.get("type"), lastException);
    }
}
