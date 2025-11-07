package com.autonova.customer.service;

import com.autonova.customer.dto.CustomerRequest;
import com.autonova.customer.dto.CustomerResponse;
import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.security.AuthenticatedUser;
import com.autonova.customer.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(99L, "admin@autonova.com", "ADMIN");
    private static final AuthenticatedUser STANDARD_USER =
            new AuthenticatedUser(7L, "user@example.com", "ROLE_USER");

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, currentUserProvider);
    }

    @Test
    void createCustomer_throwsConflictWhenEmailExists() {
        CustomerRequest request = sampleRequest("jane.doe@example.com");
        when(customerRepository.existsByEmailIgnoreCase("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomer_allowsAdminToCreateDifferentCustomer() {
        CustomerRequest request = sampleRequest("new.customer@example.com");
        when(customerRepository.existsByEmailIgnoreCase("new.customer@example.com")).thenReturn(false);
        when(currentUserProvider.requireCurrentUser()).thenReturn(ADMIN);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer toSave = invocation.getArgument(0);
            toSave.setId(123L);
            toSave.setCreatedAt(LocalDateTime.now());
            toSave.setUpdatedAt(LocalDateTime.now());
            return toSave;
        });

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.id()).isEqualTo(123L);
        assertThat(response.email()).isEqualTo("new.customer@example.com");

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new.customer@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getCustomers_nonAdminSeesOnlyOwnProfile() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(STANDARD_USER);

        Customer customer = new Customer();
        customer.setId(55L);
        customer.setEmail("user@example.com");
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setPhoneNumber("+1-555-0100");
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        when(customerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(customer));

        List<CustomerResponse> responses = customerService.getCustomers();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(55L);
        verify(customerRepository, never()).findAll();
    }

    private CustomerRequest sampleRequest(String email) {
        return new CustomerRequest("Jane", "Doe", email, "+1-555-0000");
    }
}
