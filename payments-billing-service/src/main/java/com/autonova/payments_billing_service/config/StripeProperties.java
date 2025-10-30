package com.autonova.payments_billing_service.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /**
     * Secret API key used for server-side Stripe interactions.
     */
    @NotBlank(message = "Stripe API key must be provided via STRIPE_API_KEY or configuration.")
    private String apiKey;

    /**
     * Signing secret used to validate incoming webhook payloads.
     */
    @NotBlank(message = "Stripe webhook secret must be provided via STRIPE_WEBHOOK_SECRET or configuration.")
    private String webhookSecret;

    /**
     * Publishable key for the web client; helpful for templated responses.
     */
    private String publishableKey;

    /**
     * Optional endpoint secret override when multiple webhook endpoints exist.
     */
    private String webhookEndpointSecret;
}
