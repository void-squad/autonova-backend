package com.autonova.payments_billing_service.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payments.messaging")
public class MessagingProperties {

    private final Inbound inbound = new Inbound();
    private final Outbound outbound = new Outbound();

    @Getter
    @Setter
    public static class Inbound {
        /**
         * Queue name where quote.approved events are published.
         */
        @NotBlank
        private String quoteApprovedQueue = "payments.quote-approved";
    }

    @Getter
    @Setter
    public static class Outbound {
        /**
         * Exchange used for publishing invoice.* events.
         */
        @NotBlank
        private String invoiceExchange = "billing.invoice";

        /**
         * Exchange used for publishing payment.* events.
         */
        @NotBlank
        private String paymentExchange = "billing.payment";
    }
}
