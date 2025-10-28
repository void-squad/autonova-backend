package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.api.dto.PaymentIntentResponse;
import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.autonova.payments_billing_service.domain.PaymentProvider;
import com.autonova.payments_billing_service.domain.PaymentStatus;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.PaymentRepository;
import com.autonova.payments_billing_service.stripe.StripeIntegrationException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntent.PaymentIntentStatus;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final StripeProperties stripeProperties;
    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher eventPublisher;
    private final StripeClient stripeClient;
    private final InvoiceService invoiceService;

    public PaymentService(
        StripeProperties stripeProperties,
        PaymentRepository paymentRepository,
        InvoiceService invoiceService,
        DomainEventPublisher eventPublisher
    ) {
        this.stripeProperties = stripeProperties;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.stripeClient = new StripeClient(stripeProperties.getApiKey());
        this.invoiceService = invoiceService;
    }

    @Transactional
    public PaymentIntentResponse createOrReusePaymentIntent(InvoiceEntity invoice, AuthenticatedUser user) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new IllegalStateException("Invoice has been voided");
        }

        log.debug("Preparing PaymentIntent for invoice {} by {}", invoice.getId(), user.getUserId());

        Optional<PaymentEntity> existing = paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(
            invoice.getId(),
            PaymentStatus.INITIATED
        );

        if (existing.isPresent()) {
            PaymentEntity payment = existing.get();
            PaymentIntent intent = retrievePaymentIntent(payment.getStripePaymentIntentId());
            if (intent != null && isReusable(intent)) {
                log.debug("Reusing active PaymentIntent {} for invoice {}", intent.getId(), invoice.getId());
                return new PaymentIntentResponse(intent.getId(), intent.getClientSecret(), stripeProperties.getPublishableKey());
            }
        }

        PaymentIntent newIntent = createStripePaymentIntent(invoice);
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setId(UUID.randomUUID());
        paymentEntity.setInvoice(invoice);
        paymentEntity.setAmount(invoice.getAmountTotal());
        paymentEntity.setCurrency(invoice.getCurrency());
        paymentEntity.setProvider(PaymentProvider.STRIPE);
        paymentEntity.setStatus(PaymentStatus.INITIATED);
        paymentEntity.setStripePaymentIntentId(newIntent.getId());
        paymentRepository.save(paymentEntity);

        return new PaymentIntentResponse(newIntent.getId(), newIntent.getClientSecret(), stripeProperties.getPublishableKey());
    }

    @Transactional
    public void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        PaymentEntity payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
            .orElseGet(() -> createDetachedPaymentRecord(paymentIntent));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.debug("Payment {} already marked succeeded", payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setAmount(paymentIntent.getAmountReceived() != null ? paymentIntent.getAmountReceived() : payment.getAmount());
        payment.setCurrency(paymentIntent.getCurrency());
        payment.setReceiptUrl(resolveReceiptUrl(paymentIntent));
        payment.setFailureCode(null);
        payment.setFailureMessage(null);
        paymentRepository.save(payment);

        // Refresh invoice and publish events
        InvoiceEntity invoice = payment.getInvoice();
        invoiceService.markInvoicePaid(invoice);
        eventPublisher.publishPaymentSucceeded(invoice, payment);
    }

    @Transactional
    public void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).ifPresentOrElse(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode(paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getCode() : null);
            payment.setFailureMessage(paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getMessage() : null);
            payment.setReceiptUrl(null);
            paymentRepository.save(payment);
            eventPublisher.publishPaymentFailed(payment.getId(), payment.getInvoice().getId(), payment.getFailureCode());
        }, () -> log.warn("Received payment failure for intent {} but no payment record found", paymentIntent.getId()));
    }

    @Transactional
    public void handlePaymentIntentCanceled(PaymentIntent paymentIntent) {
        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.CANCELED);
            paymentRepository.save(payment);
        });
    }

    @Transactional
    public void recordOfflinePayment(InvoiceEntity invoice, AuthenticatedUser user) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new IllegalStateException("Invoice has been voided");
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice must be finalized before recording payment");
        }

        paymentRepository.findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(invoice.getId(), PaymentStatus.SUCCEEDED)
            .ifPresent(existing -> {
                throw new IllegalStateException("Invoice already has a successful payment recorded");
            });

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setId(UUID.randomUUID());
        paymentEntity.setInvoice(invoice);
        paymentEntity.setAmount(invoice.getAmountTotal());
        paymentEntity.setCurrency(invoice.getCurrency());
        paymentEntity.setProvider(PaymentProvider.OFFLINE);
        paymentEntity.setStatus(PaymentStatus.SUCCEEDED);
        paymentEntity.setStripePaymentIntentId(null);
        paymentEntity.setFailureCode(null);
        paymentEntity.setFailureMessage(null);
        paymentEntity.setReceiptUrl(null);
        paymentRepository.save(paymentEntity);

        invoiceService.markInvoicePaid(invoice);
        eventPublisher.publishPaymentSucceeded(invoice, paymentEntity);
        log.info("Invoice {} marked as paid offline by {}", invoice.getId(), user.getUserId());
    }

    private PaymentIntent createStripePaymentIntent(InvoiceEntity invoice) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(invoice.getAmountTotal())
            .setCurrency(invoice.getCurrency().toLowerCase())
            .addPaymentMethodType("card")
            .putMetadata("invoiceId", invoice.getId().toString())
            .putMetadata("projectId", invoice.getProjectId().toString())
            .putMetadata("customerId", invoice.getCustomerId().toString())
            .build();

        try {
            return stripeClient.paymentIntents().create(params);
        } catch (StripeException e) {
            throw new StripeIntegrationException("Failed to create PaymentIntent", e);
        }
    }

    private PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return stripeClient.paymentIntents().retrieve(paymentIntentId, PaymentIntentRetrieveParams.builder().build(), null);
        } catch (StripeException e) {
            log.warn("Unable to retrieve PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            return null;
        }
    }

    private boolean isReusable(PaymentIntent paymentIntent) {
        PaymentIntentStatus status = paymentIntent.getStatus();
        return status != PaymentIntentStatus.SUCCEEDED && status != PaymentIntentStatus.CANCELED;
    }

    private PaymentEntity createDetachedPaymentRecord(PaymentIntent paymentIntent) {
        if (paymentIntent.getMetadata() == null || !paymentIntent.getMetadata().containsKey("invoiceId")) {
            throw new IllegalStateException("PaymentIntent missing invoice metadata: " + paymentIntent.getId());
        }

        UUID invoiceId = UUID.fromString(paymentIntent.getMetadata().get("invoiceId"));
        InvoiceEntity invoice = invoiceService.findById(invoiceId)
            .orElseThrow(() -> new IllegalStateException("No invoice found for PaymentIntent " + paymentIntent.getId()));

        PaymentEntity entity = new PaymentEntity();
        entity.setId(UUID.randomUUID());
        entity.setInvoice(invoice);
        entity.setAmount(paymentIntent.getAmount() != null ? paymentIntent.getAmount() : invoice.getAmountTotal());
        entity.setCurrency(paymentIntent.getCurrency());
        entity.setProvider(PaymentProvider.STRIPE);
        entity.setStatus(PaymentStatus.INITIATED);
        entity.setStripePaymentIntentId(paymentIntent.getId());
        return paymentRepository.save(entity);
    }

    private String resolveReceiptUrl(PaymentIntent paymentIntent) {
        if (paymentIntent.getCharges() == null || paymentIntent.getCharges().getData() == null) {
            return null;
        }
        return paymentIntent.getCharges().getData().stream()
            .filter(Objects::nonNull)
            .map(charge -> charge.getReceiptUrl())
            .filter(url -> url != null && !url.isBlank())
            .findFirst()
            .orElse(null);
    }
}
