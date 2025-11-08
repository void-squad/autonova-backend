package com.autonova.payments_billing_service.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthProperties {

    @NotBlank(message = "JWT HMAC secret must be provided via AUTH_JWT_HS256_SECRET")
    private String hs256Secret;
}
