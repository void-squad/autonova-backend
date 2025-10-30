package com.autonova.payments_billing_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.autonova.payments_billing_service.domain.ConsumedEventEntity;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.events.QuoteApprovedEvent;
import com.autonova.payments_billing_service.repository.ConsumedEventRepository;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import com.autonova.payments_billing_service.service.InvoiceService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.rabbitmq.listener.direct.auto-startup=false"
})
class PaymentsBillingServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("payments_billing")
        .withUsername("payments_billing")
        .withPassword("payments_billing");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ConsumedEventRepository consumedEventRepository;

    @Test
    void handlesQuoteApprovalAndProjectCompletionAcrossPersistence() {
        UUID projectId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        QuoteApprovedEvent quoteEvent = new QuoteApprovedEvent(
            UUID.randomUUID(),
            "quote.approved",
            OffsetDateTime.now(),
            1,
            new QuoteApprovedEvent.QuoteApprovedData(projectId, customerId, UUID.randomUUID(), 50_000L, "LKR", "APPROVED")
        );

        invoiceService.handleQuoteApproved(quoteEvent);

        InvoiceEntity stored = invoiceRepository.findByProjectId(projectId).orElseThrow();
        assertThat(stored.getAmountTotal()).isEqualTo(50_000L);
        assertThat(stored.getStatus()).isEqualTo(InvoiceStatus.OPEN);

        QuoteApprovedEvent updatedQuoteEvent = new QuoteApprovedEvent(
            UUID.randomUUID(),
            "quote.approved",
            OffsetDateTime.now(),
            1,
            new QuoteApprovedEvent.QuoteApprovedData(projectId, customerId, stored.getQuoteId(), 55_000L, "LKR", "APPROVED")
        );

        invoiceService.handleQuoteApproved(updatedQuoteEvent);

        InvoiceEntity refreshed = invoiceRepository.findByProjectId(projectId).orElseThrow();
        assertThat(invoiceRepository.count()).isEqualTo(1);
        assertThat(refreshed.getAmountTotal()).isEqualTo(55_000L);

        invoiceService.markInvoicePaid(refreshed);

        InvoiceEntity paidInvoice = invoiceRepository.findByProjectId(projectId).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);

        ConsumedEventEntity eventEntity = new ConsumedEventEntity(UUID.randomUUID(), "test.event", OffsetDateTime.now());
        consumedEventRepository.save(eventEntity);
        assertThatThrownBy(() -> consumedEventRepository.save(
            new ConsumedEventEntity(eventEntity.getEventId(), "another", OffsetDateTime.now()))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @TestConfiguration
    static class RabbitTestConfig {

        @Bean
        @Primary
        RabbitTemplate rabbitTemplate() {
            return Mockito.mock(RabbitTemplate.class);
        }
    }
}
