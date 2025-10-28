package com.autonova.payments_billing_service.api;

import com.autonova.payments_billing_service.config.StripeProperties;
import com.autonova.payments_billing_service.service.PaymentService;
import com.autonova.payments_billing_service.stripe.StripeIntegrationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final PaymentService paymentService;

    public StripeWebhookController(StripeProperties stripeProperties, PaymentService paymentService) {
        this.stripeProperties = stripeProperties;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
        @RequestBody String payload,
        @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        if (!StringUtils.hasText(signature)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            Event event = Webhook.constructEvent(payload, signature, resolveWebhookSecret());
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handleSucceeded(event);
                case "payment_intent.payment_failed" -> handleFailed(event);
                case "payment_intent.canceled" -> handleCanceled(event);
                default -> log.debug("Ignoring unsupported Stripe event type {}", event.getType());
            }
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException ex) {
            log.warn("Stripe webhook signature verification failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (StripeException ex) {
            throw new StripeIntegrationException("Failed to deserialize Stripe webhook event", ex);
        }
    }

    private void handleSucceeded(Event event) throws StripeException {
        PaymentIntent paymentIntent = deserializePaymentIntent(event);
        paymentService.handlePaymentIntentSucceeded(paymentIntent);
    }

    private void handleFailed(Event event) throws StripeException {
        PaymentIntent paymentIntent = deserializePaymentIntent(event);
        paymentService.handlePaymentIntentFailed(paymentIntent);
    }

    private void handleCanceled(Event event) throws StripeException {
        PaymentIntent paymentIntent = deserializePaymentIntent(event);
        paymentService.handlePaymentIntentCanceled(paymentIntent);
    }

    private PaymentIntent deserializePaymentIntent(Event event) throws StripeException {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return (PaymentIntent) deserializer.getObject().get();
        }
        return (PaymentIntent) deserializer.deserializeUnsafe();
    }

    private String resolveWebhookSecret() {
        return StringUtils.hasText(stripeProperties.getWebhookEndpointSecret())
            ? stripeProperties.getWebhookEndpointSecret()
            : Objects.requireNonNull(stripeProperties.getWebhookSecret(), "stripe webhook secret not configured");
    }
}
