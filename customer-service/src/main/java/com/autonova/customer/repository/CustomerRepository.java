package com.autonova.customer.repository;

import com.autonova.customer.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "vehicles")
    Optional<Customer> findWithVehiclesById(Long id);

    Optional<Customer> findByEmailIgnoreCase(String email);

    Optional<Customer> findByIdAndEmailIgnoreCase(Long id, String email);
}
