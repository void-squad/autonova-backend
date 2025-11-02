package com.autonova.customer.service;

import com.autonova.customer.dto.CustomerMapper;
import com.autonova.customer.dto.CustomerRequest;
import com.autonova.customer.dto.CustomerResponse;
import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this email already exists");
        }
        Customer customer = CustomerMapper.toCustomer(request);
        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toCustomerResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomers() {
        return customerRepository.findAll().stream()
                .map(CustomerMapper::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        Customer customer = customerRepository.findWithVehiclesById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        return CustomerMapper.toCustomerResponse(customer);
    }

    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A different customer already uses this email");
        }
        Customer customer = findCustomer(customerId);
        CustomerMapper.updateCustomer(customer, request);
        return CustomerMapper.toCustomerResponse(customer);
    }

    public void deleteCustomer(Long customerId) {
        Customer customer = findCustomer(customerId);
        customerRepository.delete(customer);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
