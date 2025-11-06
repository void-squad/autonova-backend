package com.autonova.auth_service.user.service;

import com.autonova.auth_service.user.dto.CustomerProfile;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client used to retrieve customer profiles from the customer-service.
 */
@Component
public class CustomerServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceClient.class);

    private final RestTemplate restTemplate;
    private final String customerServiceBaseUrl;

    public CustomerServiceClient(RestTemplate restTemplate,
            @Value("${customer.service.base-url:http://localhost:8083}") String customerServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceBaseUrl = customerServiceBaseUrl;
    }

    public Optional<CustomerProfile> getCurrentCustomer(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            LOGGER.debug("Skipping customer lookup because Authorization header is missing");
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CustomerProfile> response = restTemplate.exchange(
                    customerServiceBaseUrl + "/api/customers/me",
                    HttpMethod.GET,
                    entity,
                    CustomerProfile.class);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            LOGGER.debug("Customer profile not found for token: {}", ex.getMessage());
            return Optional.empty();
        } catch (HttpClientErrorException ex) {
            LOGGER.warn("Failed to fetch customer profile: status={} message={}", ex.getStatusCode(), ex.getMessage());
            return Optional.empty();
        }
    }
}
