package com.autonova.customer.integration;

import com.autonova.customer.model.Customer;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Customer Service with PostgreSQL and RabbitMQ using Testcontainers
 */
@SpringBootTest
@Testcontainers
@Transactional
class CustomerServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("customerdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("jwt.secret", () -> "testSecretKeyForJwtTokenGenerationAndValidationInTestEnvironmentMinimum256Bits");
        registry.add("jwt.expiration", () -> "3600000");
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void testContainersAreRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    void createAndRetrieveCustomer_worksCorrectly() {
        // Given
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhoneNumber("123-456-7890");
        customer = customerRepository.save(customer);

        // When
        Customer retrieved = customerRepository.findById(customer.getId()).orElse(null);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFirstName()).isEqualTo("John");
        assertThat(retrieved.getLastName()).isEqualTo("Doe");
        assertThat(retrieved.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void updateCustomer_persistsChanges() {
        // Given
        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane.smith@example.com");
        customer.setPhoneNumber("111-222-3333");
        customer = customerRepository.save(customer);

        // When
        customer.setPhoneNumber("999-888-7777");
        customer.setFirstName("Janet");
        customerRepository.save(customer);

        // Then
        Customer updated = customerRepository.findById(customer.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getFirstName()).isEqualTo("Janet");
        assertThat(updated.getPhoneNumber()).isEqualTo("999-888-7777");
    }

    @Test
    void deleteCustomer_removesFromDatabase() {
        // Given
        Customer customer = new Customer();
        customer.setFirstName("Bob");
        customer.setLastName("Johnson");
        customer.setEmail("bob.johnson@example.com");
        customer.setPhoneNumber("444-555-6666");
        customer = customerRepository.save(customer);
        Long customerId = customer.getId();

        // When
        customerRepository.deleteById(customerId);

        // Then
        assertThat(customerRepository.findById(customerId)).isEmpty();
    }

    @Test
    void findByEmail_returnsCorrectCustomer() {
        // Given
        Customer customer1 = new Customer();
        customer1.setFirstName("Alice");
        customer1.setLastName("Williams");
        customer1.setEmail("alice@example.com");
        customer1.setPhoneNumber("777-888-9999");
        customerRepository.save(customer1);

        Customer customer2 = new Customer();
        customer2.setFirstName("Charlie");
        customer2.setLastName("Brown");
        customer2.setEmail("charlie@example.com");
        customer2.setPhoneNumber("111-222-3333");
        customerRepository.save(customer2);

        // When
        Customer found = customerRepository.findByEmailIgnoreCase("alice@example.com").orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("Alice");
        assertThat(found.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void multipleCustomers_canBeStoredAndRetrieved() {
        // Given
        for (int i = 0; i < 5; i++) {
            Customer customer = new Customer();
            customer.setFirstName("Customer" + i);
            customer.setLastName("Test" + i);
            customer.setEmail("customer" + i + "@example.com");
            customer.setPhoneNumber("555-000-" + String.format("%04d", i));
            customerRepository.save(customer);
        }

        // When
        long count = customerRepository.count();

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    void emailUniquenessConstraint_isEnforced() {
        // Given
        Customer customer1 = new Customer();
        customer1.setFirstName("Test");
        customer1.setLastName("User");
        customer1.setEmail("test@example.com");
        customer1.setPhoneNumber("123-456-7890");
        customerRepository.save(customer1);

        // When
        boolean exists = customerRepository.existsByEmailIgnoreCase("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }
}
