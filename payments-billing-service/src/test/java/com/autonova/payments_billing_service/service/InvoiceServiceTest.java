package com.autonova.payments_billing_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, eventPublisher);
    }

    @Test
    void createInvoiceStoresCallerContext() {
        UUID projectId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(42L, "customer@example.com", Set.of("customer"));
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            projectId,
            UUID.randomUUID(),
            "Project Falcon",
            "Migration work",
            12_500L,
            "LKR"
        );

        when(invoiceRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.createInvoice(command, user);

        verify(invoiceRepository).save(argThat(invoice -> {
            assertThat(invoice.getProjectId()).isEqualTo(projectId);
            assertThat(invoice.getCustomerEmail()).isEqualTo("customer@example.com");
            assertThat(invoice.getCustomerUserId()).isEqualTo(42L);
            assertThat(invoice.getProjectName()).isEqualTo("Project Falcon");
            assertThat(invoice.getProjectDescription()).isEqualTo("Migration work");
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
            assertThat(invoice.getCurrency()).isEqualTo("lkr");
            assertThat(invoice.getAmountTotal()).isEqualTo(12_500L);
            return true;
        }));
        verify(eventPublisher).publishInvoiceCreated(any(InvoiceEntity.class));
    }

    @Test
    void createInvoiceRejectsDuplicateProject() {
        UUID projectId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(42L, "customer@example.com", Set.of("customer"));
        CreateInvoiceCommand command = new CreateInvoiceCommand(projectId, null, "Project", null, 10_000L, "LKR");

        when(invoiceRepository.findByProjectId(projectId)).thenReturn(Optional.of(new InvoiceEntity()));

        assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(command, user));
        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publishInvoiceCreated(any());
    }

    @Test
    void getInvoiceForUserRejectsCustomerAccessingOtherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(invoiceId);
        invoice.setProjectId(UUID.randomUUID());
        invoice.setCustomerEmail("owner@example.com");
        invoice.setCustomerUserId(7L);
        invoice.setStatus(InvoiceStatus.OPEN);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        AuthenticatedUser otherCustomer = new AuthenticatedUser(99L, "other@example.com", Set.of("customer"));

        assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> invoiceService.getInvoiceForUser(invoiceId, otherCustomer)
        );
    }
}
