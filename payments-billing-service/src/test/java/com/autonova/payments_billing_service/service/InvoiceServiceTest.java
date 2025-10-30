package com.autonova.payments_billing_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.ConsumedEventEntity;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.events.QuoteApprovedEvent;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.ConsumedEventRepository;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ConsumedEventRepository consumedEventRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, consumedEventRepository, eventPublisher);
    }

    @Test
    void handleQuoteApprovedCreatesInvoiceWhenNoneExists() {
        UUID projectId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        QuoteApprovedEvent event = new QuoteApprovedEvent(
            UUID.randomUUID(),
            "quote.approved",
            OffsetDateTime.now(),
            1,
            new QuoteApprovedEvent.QuoteApprovedData(projectId, customerId, UUID.randomUUID(), 12_500L, "lkr", "APPROVED")
        );

        when(consumedEventRepository.save(any(ConsumedEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.handleQuoteApproved(event);

        verify(invoiceRepository).save(argThat(invoice -> {
            assertThat(invoice.getProjectId()).isEqualTo(projectId);
            assertThat(invoice.getCustomerId()).isEqualTo(customerId);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
            assertThat(invoice.getCurrency()).isEqualTo("LKR");
            assertThat(invoice.getAmountTotal()).isEqualTo(12_500L);
            return true;
        }));
        verify(eventPublisher).publishInvoiceCreated(any(InvoiceEntity.class));
    }

    @Test
    void handleQuoteApprovedSkipsWhenEventAlreadyConsumed() {
        QuoteApprovedEvent event = new QuoteApprovedEvent(
            UUID.randomUUID(),
            "quote.approved",
            OffsetDateTime.now(),
            1,
            new QuoteApprovedEvent.QuoteApprovedData(UUID.randomUUID(), UUID.randomUUID(), null, 10_000L, "LKR", "APPROVED")
        );

        when(consumedEventRepository.save(any(ConsumedEventEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        invoiceService.handleQuoteApproved(event);

        verify(invoiceRepository, never()).findByProjectId(any());
        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publishInvoiceCreated(any());
    }

    @Test
    void getInvoiceForUserRejectsCustomerAccessingOtherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(invoiceId);
        invoice.setProjectId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.OPEN);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        AuthenticatedUser otherCustomer = new AuthenticatedUser(UUID.randomUUID(), Set.of("customer"));

        assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> invoiceService.getInvoiceForUser(invoiceId, otherCustomer)
        );
    }
}
