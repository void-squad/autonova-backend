package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.*;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.auth.AuthenticatedUser;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceOfflineRecordTest {

	@Mock
	StripeProperties props;

	@Mock
	PaymentRepository paymentRepository;

	@Mock
	DomainEventPublisher publisher;

	@Mock
	InvoiceService invoiceService;

	private PaymentService paymentService;

	@BeforeEach
	void setup(){
		when(props.getApiKey()).thenReturn("sk_test_key");
		paymentService = new PaymentService(props, paymentRepository, invoiceService, publisher);
	}

	@Test
	void recordOfflinePayment_createsEntityAndPublishes(){
		InvoiceEntity invoice = new InvoiceEntity();
		invoice.setId(UUID.randomUUID());
		invoice.setProjectId(UUID.randomUUID());
		invoice.setCustomerEmail("c@example.com");
		invoice.setCustomerUserId(10L);
		invoice.setCurrency("lkr");
		invoice.setAmountTotal(5000L);
		invoice.setStatus(InvoiceStatus.OPEN);
		when(paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoice.getId(), PaymentStatus.SUCCEEDED))
			.thenReturn(Optional.empty());
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
		AuthenticatedUser user = new AuthenticatedUser(1L, "test@example.com", Set.of("CUSTOMER"));
		paymentService.recordOfflinePayment(invoice, user);
		ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository).save(captor.capture());
		PaymentEntity saved = captor.getValue();
		assertThat(saved.getProvider()).isEqualTo(PaymentProvider.OFFLINE);
		assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
		verify(invoiceService).markInvoicePaid(invoice);
		verify(publisher).publishPaymentSucceeded(eq(invoice), any(PaymentEntity.class));
	}
}