package com.autonova.payments_billing_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.autonova.payments_billing_service.domain.PaymentProvider;
import com.autonova.payments_billing_service.domain.PaymentStatus;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.PaymentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private DomainEventPublisher eventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties();
        stripeProperties.setApiKey("sk_test_dummy");
        stripeProperties.setWebhookSecret("whsec_dummy");
        stripeProperties.setPublishableKey("pk_test_dummy");

        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
    }

    @Test
    void recordOfflinePayment_persistsPaymentAndMarksInvoicePaid() {
        InvoiceEntity invoice = buildInvoice(InvoiceStatus.OPEN);
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoice.getId(), PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(invoiceService).markInvoicePaid(invoice);

        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), Set.of("EMPLOYEE"));

        paymentService.recordOfflinePayment(invoice, user);

        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentProvider.OFFLINE, savedPayment.getProvider());
        assertEquals(PaymentStatus.SUCCEEDED, savedPayment.getStatus());
        assertEquals(invoice.getAmountTotal(), savedPayment.getAmount());
        assertEquals(invoice.getCurrency(), savedPayment.getCurrency());
        assertEquals(invoice, savedPayment.getInvoice());

        verify(invoiceService).markInvoicePaid(invoice);
        verify(eventPublisher).publishPaymentSucceeded(invoice, savedPayment);
    }

    @Test
    void recordOfflinePayment_rejectsIfInvoiceAlreadyPaid() {
        InvoiceEntity invoice = buildInvoice(InvoiceStatus.PAID);
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), Set.of("EMPLOYEE"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> paymentService.recordOfflinePayment(invoice, user)
        );

        assertEquals("Invoice already paid", exception.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).markInvoicePaid(any());
    }

    @Test
    void recordOfflinePayment_rejectsIfInvoiceDraft() {
        InvoiceEntity invoice = buildInvoice(InvoiceStatus.DRAFT);
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), Set.of("EMPLOYEE"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> paymentService.recordOfflinePayment(invoice, user)
        );

        assertEquals("Invoice must be finalized before recording payment", exception.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).markInvoicePaid(any());
    }

    @Test
    void recordOfflinePayment_rejectsIfSuccessfulPaymentExists() {
        InvoiceEntity invoice = buildInvoice(InvoiceStatus.OPEN);
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoice.getId(), PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.of(new PaymentEntity()));

        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), Set.of("EMPLOYEE"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> paymentService.recordOfflinePayment(invoice, user)
        );

        assertEquals("Invoice already has a successful payment recorded", exception.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).markInvoicePaid(any());
    }

    private InvoiceEntity buildInvoice(InvoiceStatus status) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setProjectId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmountTotal(5_000L);
        invoice.setCurrency("LKR");
        invoice.setStatus(status);
        return invoice;
    }
}
