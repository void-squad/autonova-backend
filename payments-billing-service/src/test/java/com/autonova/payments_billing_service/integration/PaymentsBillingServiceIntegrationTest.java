package com.autonova.payments_billing_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import com.autonova.payments_billing_service.service.CreateInvoiceCommand;
import com.autonova.payments_billing_service.service.InvoiceService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    @Test
    void createsInvoiceForCustomerAndMarksPaid() {
        UUID projectId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(5L, "customer@example.com", Set.of("customer"));
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            projectId,
            UUID.randomUUID(),
            "Project Atlas",
            "Full site redesign",
            50_000L,
            "LKR"
        );

        InvoiceEntity created = invoiceService.createInvoice(command, user);

        InvoiceEntity stored = invoiceRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getCustomerEmail()).isEqualTo("customer@example.com");
        assertThat(stored.getCustomerUserId()).isEqualTo(5L);
        assertThat(stored.getProjectName()).isEqualTo("Project Atlas");
        assertThat(stored.getProjectDescription()).isEqualTo("Full site redesign");
        assertThat(stored.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(stored.getCurrency()).isEqualTo("lkr");

        invoiceService.markInvoicePaid(stored);

        InvoiceEntity paidInvoice = invoiceRepository.findById(created.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void preventsCreatingDuplicateInvoiceForProject() {
        UUID projectId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(8L, "dupe@example.com", Set.of("customer"));
        CreateInvoiceCommand command = new CreateInvoiceCommand(projectId, null, "Project", null, 25_000L, "LKR");

        invoiceService.createInvoice(command, user);

        assertThatThrownBy(() -> invoiceService.createInvoice(command, user))
            .isInstanceOf(IllegalStateException.class);
        assertThat(invoiceRepository.count()).isEqualTo(1);
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
