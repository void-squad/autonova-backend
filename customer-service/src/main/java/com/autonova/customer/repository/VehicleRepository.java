package com.autonova.customer.repository;

import com.autonova.customer.model.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByCustomerIdAndLicensePlateIgnoreCase(Long customerId, String licensePlate);

    boolean existsByCustomerIdAndLicensePlateIgnoreCaseAndIdNot(Long customerId, String licensePlate, Long id);

    boolean existsByVinIgnoreCase(String vin);

    boolean existsByVinIgnoreCaseAndIdNot(String vin, Long id);

    Optional<Vehicle> findByCustomerIdAndId(Long customerId, Long vehicleId);

    List<Vehicle> findAllByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);

    /**
     * Find vehicle by ID (for cross-service consumption).
     * Used by other services (e.g., Project Service) to fetch vehicle details.
     */
    Optional<Vehicle> findById(Long vehicleId);
}
