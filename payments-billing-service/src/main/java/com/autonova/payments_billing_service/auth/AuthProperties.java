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

    /**
     * Expected JWT issuer. Allows simple validation prior to trusting token.
     */
    @NotBlank(message = "JWT issuer must be provided via AUTH_JWT_ISSUER")
    private String issuer;

    /**
     * Expected JWT audience.
     */
    @NotBlank(message = "JWT audience must be provided via AUTH_JWT_AUDIENCE")
    private String audience;

    @NotBlank(message = "JWT HMAC secret must be provided via AUTH_JWT_HS256_SECRET")
    private String hs256Secret;
}
