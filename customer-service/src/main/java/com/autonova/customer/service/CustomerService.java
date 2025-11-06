package com.autonova.customer.service;

import com.autonova.customer.dto.CustomerMapper;
import com.autonova.customer.dto.CustomerRequest;
import com.autonova.customer.dto.CustomerResponse;
import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.security.AuthenticatedUser;
import com.autonova.customer.security.CurrentUserProvider;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CurrentUserProvider currentUserProvider;

    public CustomerService(CustomerRepository customerRepository, CurrentUserProvider currentUserProvider) {
        this.customerRepository = customerRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this email already exists");
        }

        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (!currentUser.hasRole("ADMIN")
                && !request.email().trim().equalsIgnoreCase(currentUser.normalizedEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create your own customer profile");
        }
        Customer customer = CustomerMapper.toCustomer(request);
        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toCustomerResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomers() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (currentUser.hasRole("ADMIN")) {
            return customerRepository.findAll().stream()
                    .map(CustomerMapper::toCustomerResponse)
                    .toList();
        }

        return customerRepository.findByEmailIgnoreCase(currentUser.normalizedEmail())
                .map(CustomerMapper::toCustomerResponse)
                .map(List::of)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        Customer customer = customerRepository.findWithVehiclesById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        ensureCustomerAccess(customer);
        return CustomerMapper.toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomer() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        Customer customer = customerRepository.findByEmailIgnoreCase(currentUser.normalizedEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
        return CustomerMapper.toCustomerResponse(customer);
    }

    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A different customer already uses this email");
        }
        Customer customer = findCustomer(customerId);
        ensureCustomerAccess(customer);
        CustomerMapper.updateCustomer(customer, request);
        return CustomerMapper.toCustomerResponse(customer);
    }

    public void deleteCustomer(Long customerId) {
        Customer customer = findCustomer(customerId);
        ensureCustomerAccess(customer);
        customerRepository.delete(customer);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void ensureCustomerAccess(Customer customer) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (currentUser.hasRole("ADMIN")) {
            return;
        }

        if (!customer.getEmail().equalsIgnoreCase(currentUser.normalizedEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for customer");
        }
    }
}
