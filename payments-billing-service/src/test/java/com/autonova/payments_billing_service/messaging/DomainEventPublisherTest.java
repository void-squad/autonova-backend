package com.autonova.payments_billing_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.autonova.payments_billing_service.config.MessagingProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DomainEventPublisher publisher;
    private MessagingProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new MessagingProperties();
        objectMapper = new ObjectMapper();
        publisher = new DomainEventPublisher(rabbitTemplate, properties, objectMapper);
    }

    @Test
    void publishInvoiceCreatedMatchesContractShape() throws Exception {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setProjectId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmountTotal(42_500L);
        invoice.setCurrency("LKR");
        invoice.setStatus(InvoiceStatus.OPEN);

        publisher.publishInvoiceCreated(invoice);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
            eq(properties.getOutbound().getInvoiceExchange()),
            eq("invoice.created"),
            payloadCaptor.capture()
        );

        Map<String, Object> payload = objectMapper.readValue(payloadCaptor.getValue(), new TypeReference<>() {});
        assertThat(payload.get("type")).isEqualTo("invoice.created");
        assertThat(payload.get("version")).isEqualTo(1);
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        assertThat(data).containsKeys("invoice_id", "project_id", "customer_id", "amount_total", "currency", "status");
    }

    @Test
    void publishInvoiceUpdatedRetriesOnTransientFailure() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setProjectId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmountTotal(42_500L);
        invoice.setCurrency("LKR");
        invoice.setStatus(InvoiceStatus.OPEN);

        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() < 2) {
                throw new AmqpException("broker unavailable");
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        publisher.publishInvoiceUpdated(invoice);

        assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
    }
}
