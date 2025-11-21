package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.autonova.payments_billing_service.domain.PaymentProvider;
import com.autonova.payments_billing_service.domain.PaymentStatus;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    DomainEventPublisher eventPublisher;

    StripeProperties stripeProperties;

    PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stripeProperties = new StripeProperties();
        stripeProperties.setApiKey("sk_test_xxx");
        stripeProperties.setPublishableKey("pk_test_xxx");

        // invoiceService is not needed for the tested methods here; pass null where allowed
        paymentService = new PaymentService(stripeProperties, paymentRepository, null, eventPublisher);
    }

    @Test
    void findLatestSuccessfulPaymentProvider_returnsProvider_whenPresent() {
        UUID invoiceId = UUID.randomUUID();
        PaymentEntity entity = new PaymentEntity();
        entity.setProvider(PaymentProvider.OFFLINE);
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoiceId, PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.of(entity));

        Optional<PaymentProvider> opt = paymentService.findLatestSuccessfulPaymentProvider(invoiceId);

        assertThat(opt).isPresent().contains(PaymentProvider.OFFLINE);
    }

    @Test
    void recordOfflinePayment_throwsWhenInvoicePaid() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PAID);

        assertThrows(IllegalStateException.class, () -> paymentService.recordOfflinePayment(invoice, null));
    }
}
package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.*;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private StripeProperties stripeProperties;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private DomainEventPublisher eventPublisher;

    private PaymentService paymentService;
    private AuthenticatedUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AuthenticatedUser(1L, "test@example.com", Set.of("CUSTOMER"));
        // Note: PaymentService constructor creates StripeClient, so we need a valid API key
        when(stripeProperties.getApiKey()).thenReturn("sk_test_dummy_key");
        // Skip creating the actual service for tests that don't need Stripe
    }

    @Test
    void recordOfflinePayment_withValidInvoice_createsPaymentAndMarksInvoicePaid() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = createTestInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.OPEN);
        
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(
            invoiceId, PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.recordOfflinePayment(invoice, testUser);

        // Then
        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        
        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getInvoice()).isEqualTo(invoice);
        assertThat(savedPayment.getProvider()).isEqualTo(PaymentProvider.OFFLINE);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(savedPayment.getAmount()).isEqualTo(invoice.getAmountTotal());
        assertThat(savedPayment.getCurrency()).isEqualTo(invoice.getCurrency());
        assertThat(savedPayment.getStripePaymentIntentId()).isNull();
        
        verify(invoiceService).markInvoicePaid(invoice);
        verify(eventPublisher).publishPaymentSucceeded(eq(invoice), any(PaymentEntity.class));
    }

    @Test
    void recordOfflinePayment_alreadyPaidInvoice_throwsIllegalStateException() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        InvoiceEntity invoice = createTestInvoice(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PAID);

        // When/Then
        assertThatThrownBy(() -> paymentService.recordOfflinePayment(invoice, testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invoice already paid");
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordOfflinePayment_voidInvoice_throwsIllegalStateException() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        InvoiceEntity invoice = createTestInvoice(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.VOID);

        // When/Then
        assertThatThrownBy(() -> paymentService.recordOfflinePayment(invoice, testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invoice has been voided");
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordOfflinePayment_draftInvoice_throwsIllegalStateException() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        InvoiceEntity invoice = createTestInvoice(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.DRAFT);

        // When/Then
        assertThatThrownBy(() -> paymentService.recordOfflinePayment(invoice, testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invoice must be finalized before recording payment");
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordOfflinePayment_invoiceWithExistingSuccessfulPayment_throwsIllegalStateException() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = createTestInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.OPEN);
        
        PaymentEntity existingPayment = new PaymentEntity();
        existingPayment.setStatus(PaymentStatus.SUCCEEDED);
        
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(
            invoiceId, PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.of(existingPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.recordOfflinePayment(invoice, testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invoice already has a successful payment recorded");
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void findLatestSuccessfulPaymentProvider_withSuccessfulPayment_returnsProvider() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        UUID invoiceId = UUID.randomUUID();
        PaymentEntity payment = new PaymentEntity();
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(
            invoiceId, PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.of(payment));

        // When
        Optional<PaymentProvider> result = paymentService.findLatestSuccessfulPaymentProvider(invoiceId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    void findLatestSuccessfulPaymentProvider_withNoSuccessfulPayment_returnsEmpty() {
        // Given
        paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
        
        UUID invoiceId = UUID.randomUUID();
        
        when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(
            invoiceId, PaymentStatus.SUCCEEDED))
            .thenReturn(Optional.empty());

        // When
        Optional<PaymentProvider> result = paymentService.findLatestSuccessfulPaymentProvider(invoiceId);

        // Then
        assertThat(result).isEmpty();
    }

    private InvoiceEntity createTestInvoice(UUID invoiceId) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(invoiceId);
        invoice.setProjectId(UUID.randomUUID());
        invoice.setQuoteId(UUID.randomUUID());
        invoice.setProjectName("Test Project");
        invoice.setProjectDescription("Test Description");
        invoice.setCustomerEmail("test@example.com");
        invoice.setCustomerUserId(100L);
        invoice.setCurrency("lkr");
        invoice.setAmountTotal(10000L);
        invoice.setStatus(InvoiceStatus.OPEN);
        return invoice;
    }
}
