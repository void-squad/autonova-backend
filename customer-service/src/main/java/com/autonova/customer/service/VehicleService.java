package com.autonova.customer.service;

import com.autonova.customer.dto.CustomerMapper;
import com.autonova.customer.dto.VehicleRequest;
import com.autonova.customer.dto.VehicleResponse;
import com.autonova.customer.model.Customer;
import com.autonova.customer.model.Vehicle;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.repository.VehicleRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class VehicleService {

    private static final Comparator<Vehicle> VEHICLE_COMPARATOR =
            Comparator.comparing(Vehicle::getId, Comparator.nullsLast(Long::compare));

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleService(CustomerRepository customerRepository, VehicleRepository vehicleRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponse addVehicle(Long customerId, VehicleRequest request) {
        Customer customer = findCustomer(customerId);
        String normalizedPlate = normalizeLicensePlate(request.licensePlate());
        String normalizedVin = normalizeVin(request.vin());

        if (vehicleRepository.existsByVinIgnoreCase(normalizedVin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vehicle with this VIN already exists");
        }

        if (vehicleRepository.existsByCustomerIdAndLicensePlateIgnoreCase(customerId, normalizedPlate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This license plate is already registered for the customer");
        }

        Vehicle vehicle = CustomerMapper.toVehicle(request);
        vehicle.setLicensePlate(normalizedPlate);
        vehicle.setVin(normalizedVin);
        customer.addVehicle(vehicle);

        try {
            Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
            return CustomerMapper.toVehicleResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles(Long customerId) {
        assertCustomerExists(customerId);
        return vehicleRepository.findAllByCustomerId(customerId).stream()
                .sorted(VEHICLE_COMPARATOR)
                .map(CustomerMapper::toVehicleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long customerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        return CustomerMapper.toVehicleResponse(vehicle);
    }

    public VehicleResponse updateVehicle(Long customerId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        String normalizedPlate = normalizeLicensePlate(request.licensePlate());
        String normalizedVin = normalizeVin(request.vin());

        if (vehicleRepository.existsByVinIgnoreCaseAndIdNot(normalizedVin, vehicleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another vehicle already uses this VIN");
        }

        if (vehicleRepository.existsByCustomerIdAndLicensePlateIgnoreCaseAndIdNot(customerId, normalizedPlate, vehicleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another vehicle for this customer already uses this license plate");
        }

        CustomerMapper.updateVehicle(vehicle, request);
        vehicle.setLicensePlate(normalizedPlate);
        vehicle.setVin(normalizedVin);

        try {
            Vehicle updated = vehicleRepository.saveAndFlush(vehicle);
            return CustomerMapper.toVehicleResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
    }

    public void deleteVehicle(Long customerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        Customer customer = vehicle.getCustomer();
        if (customer != null) {
            customer.removeVehicle(vehicle);
        }
        vehicleRepository.delete(vehicle);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void assertCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
    }

    private String normalizeLicensePlate(String licensePlate) {
        return licensePlate == null ? null : licensePlate.trim().toUpperCase();
    }

    private String normalizeVin(String vin) {
        return vin == null ? null : vin.trim().toUpperCase();
    }

    private ResponseStatusException translateIntegrityViolation(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (detail != null) {
            String normalizedDetail = detail.toLowerCase();
            if (normalizedDetail.contains("uk_vehicle_vin")) {
                return new ResponseStatusException(HttpStatus.CONFLICT, "A vehicle with this VIN already exists", ex);
            }
            if (normalizedDetail.contains("uk_vehicle_customer_license")) {
                return new ResponseStatusException(HttpStatus.CONFLICT, "This license plate is already registered for the customer", ex);
            }
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate vehicle detected", ex);
    }
}
