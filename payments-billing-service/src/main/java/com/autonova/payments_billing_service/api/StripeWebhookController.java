package com.autonova.payments_billing_service.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    @PostMapping
    public ResponseEntity<Void> handleWebhook() {
        // TODO: wire webhook processing flow.
        return ResponseEntity.accepted().build();
    }
}
