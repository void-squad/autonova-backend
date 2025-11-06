package com.autonova.customer.service;

import com.autonova.customer.event.auth.AuthUserLoggedInEvent;
import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Synchronises customer records with authenticated user sessions emitted by
 * auth-service.
 */
@Service
@Transactional
public class AuthenticatedUserSyncService {

  private static final int MAX_NAME_LENGTH = 60;
  private static final int MAX_PHONE_LENGTH = 30;
  private static final String DEFAULT_NAME_TOKEN = "Unknown";
  private static final String DEFAULT_PHONE = "UNAVAILABLE";

  private final CustomerRepository customerRepository;
  private final Logger logger = LoggerFactory.getLogger(AuthenticatedUserSyncService.class);

  public AuthenticatedUserSyncService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public void syncCustomer(AuthUserLoggedInEvent event) {
    if (event == null) {
      logger.warn("Received null auth login event; ignoring");
      return;
    }

    String email = normalizeEmail(event.email());
    if (!StringUtils.hasText(email)) {
      logger.warn("Discarding auth login event without email: {}", event);
      return;
    }

    Customer customer = findOrCreateCustomer(email);
    customer.setEmail(email);
    customer.setFirstName(normalizeName(event.firstName()));
    customer.setLastName(normalizeName(event.lastName()));
    customer.setPhoneNumber(normalizePhone(event.phoneNumber()));

    if (customer.getId() == null) {
      customerRepository.save(customer);
      logger.info("Created customer profile for auth user {}", email);
    } else {
      logger.debug("Updated customer profile for auth user {}", email);
    }
  }

  private Customer findOrCreateCustomer(String normalizedEmail) {
    Optional<Customer> existing = customerRepository.findByEmailIgnoreCase(normalizedEmail);
    if (existing.isPresent()) {
      return existing.get();
    }
    Customer customer = new Customer();
    customer.setEmail(normalizedEmail);
    customer.setFirstName(DEFAULT_NAME_TOKEN);
    customer.setLastName(DEFAULT_NAME_TOKEN);
    customer.setPhoneNumber(DEFAULT_PHONE);
    return customer;
  }

  private String normalizeEmail(String email) {
    return StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
  }

  private String normalizeName(String value) {
    if (!StringUtils.hasText(value)) {
      return DEFAULT_NAME_TOKEN;
    }
    String trimmed = value.trim();
    return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
  }

  private String normalizePhone(String value) {
    if (!StringUtils.hasText(value)) {
      return DEFAULT_PHONE;
    }
    String trimmed = value.trim();
    return trimmed.length() > MAX_PHONE_LENGTH ? trimmed.substring(0, MAX_PHONE_LENGTH) : trimmed;
  }
}
