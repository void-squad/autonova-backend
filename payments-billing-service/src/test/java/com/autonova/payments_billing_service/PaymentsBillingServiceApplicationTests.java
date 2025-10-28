package com.autonova.payments_billing_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentsBillingServiceApplicationTests {

    @Test
    void contextLoads() {
        // Ensures core configuration boots without touching external systems.
    }
}
