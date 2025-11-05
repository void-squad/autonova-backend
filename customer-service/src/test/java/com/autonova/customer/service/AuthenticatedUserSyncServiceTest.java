package com.autonova.customer.service;

import com.autonova.customer.event.auth.AuthUserLoggedInEvent;
import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserSyncServiceTest {

  @Mock
  private CustomerRepository customerRepository;

  @InjectMocks
  private AuthenticatedUserSyncService syncService;

  @Test
  void createsCustomerWhenMissing() {
    when(customerRepository.findByEmailIgnoreCase("ada.lovelace@example.com"))
        .thenReturn(Optional.empty());

    AuthUserLoggedInEvent event = sampleEvent(
        "Ada.Lovelace@example.com",
        " Ada ",
        " Lovelace ",
        "+1-555-0100");

    syncService.syncCustomer(event);

    ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
    verify(customerRepository).save(captor.capture());
    Customer saved = captor.getValue();
    assertThat(saved.getEmail()).isEqualTo("ada.lovelace@example.com");
    assertThat(saved.getFirstName()).isEqualTo("Ada");
    assertThat(saved.getLastName()).isEqualTo("Lovelace");
    assertThat(saved.getPhoneNumber()).isEqualTo("+1-555-0100");
  }

  @Test
  void updatesExistingCustomerOnLogin() {
    Customer existing = new Customer();
    existing.setId(42L);
    existing.setEmail("grace.hopper@example.com");
    existing.setFirstName("Grace");
    existing.setLastName("Hopper");
    existing.setPhoneNumber("+1-555-0199");

    when(customerRepository.findByEmailIgnoreCase("grace.hopper@example.com"))
        .thenReturn(Optional.of(existing));

    AuthUserLoggedInEvent event = sampleEvent(
        "grace.hopper@example.com",
        "Grace",
        "Updated",
        "+1-555-0000");

    syncService.syncCustomer(event);

    verify(customerRepository, never()).save(any(Customer.class));
    assertThat(existing.getFirstName()).isEqualTo("Grace");
    assertThat(existing.getLastName()).isEqualTo("Updated");
    assertThat(existing.getPhoneNumber()).isEqualTo("+1-555-0000");
  }

  private AuthUserLoggedInEvent sampleEvent(String email, String firstName, String lastName, String phoneNumber) {
    return new AuthUserLoggedInEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        email,
        firstName,
        lastName,
        phoneNumber,
        Set.of("ROLE_USER"),
        Instant.now());
  }
}
