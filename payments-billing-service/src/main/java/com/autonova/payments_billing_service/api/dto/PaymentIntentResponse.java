package com.autonova.payments_billing_service.api.dto;

public record PaymentIntentResponse(
    String paymentIntentId,
    String clientSecret,
    String publishableKey
) {
}
