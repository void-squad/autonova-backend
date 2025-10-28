package com.autonova.payments_billing_service;

import com.autonova.payments_billing_service.config.MessagingProperties;
import com.autonova.payments_billing_service.config.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = {StripeProperties.class, MessagingProperties.class})
public class PaymentsBillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsBillingServiceApplication.class, args);
    }
}
