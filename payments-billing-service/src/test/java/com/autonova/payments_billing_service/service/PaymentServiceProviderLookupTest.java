package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.*;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceProviderLookupTest {

	@Mock
	StripeProperties stripeProperties;

	@Mock
	PaymentRepository paymentRepository;

	@Mock
	DomainEventPublisher eventPublisher;

	@Mock
	InvoiceService invoiceService;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		when(stripeProperties.getApiKey()).thenReturn("sk_test_key");
		paymentService = new PaymentService(stripeProperties, paymentRepository, invoiceService, eventPublisher);
	}

	@Test
	void findLatestSuccessfulPaymentProvider_returnsEmpty_whenNone() {
		UUID invoiceId = UUID.randomUUID();
		when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoiceId, PaymentStatus.SUCCEEDED))
				.thenReturn(Optional.empty());
		assertThat(paymentService.findLatestSuccessfulPaymentProvider(invoiceId)).isEmpty();
	}

	@Test
	void findLatestSuccessfulPaymentProvider_returnsProvider_whenPresent() {
		UUID invoiceId = UUID.randomUUID();
		PaymentEntity entity = new PaymentEntity();
		entity.setProvider(PaymentProvider.STRIPE);
		when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoiceId, PaymentStatus.SUCCEEDED))
				.thenReturn(Optional.of(entity));
		assertThat(paymentService.findLatestSuccessfulPaymentProvider(invoiceId)).contains(PaymentProvider.STRIPE);
	}
}