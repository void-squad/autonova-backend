package com.autonova.customer.service;

import com.autonova.customer.dto.CustomerMapper;
import com.autonova.customer.dto.VehicleRequest;
import com.autonova.customer.dto.VehicleResponse;
import com.autonova.customer.dto.VehicleStatsResponse;
import com.autonova.customer.event.VehicleDomainEventPublisher;
import com.autonova.customer.event.VehicleEventType;
import com.autonova.customer.model.Customer;
import com.autonova.customer.model.Vehicle;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.repository.VehicleRepository;
import com.autonova.customer.security.AuthenticatedUser;
import com.autonova.customer.security.CurrentUserProvider;
import java.time.Instant;
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
    private final VehicleDomainEventPublisher vehicleDomainEventPublisher;
    private final CurrentUserProvider currentUserProvider;

    public VehicleService(CustomerRepository customerRepository,
                          VehicleRepository vehicleRepository,
                          VehicleDomainEventPublisher vehicleDomainEventPublisher,
                          CurrentUserProvider currentUserProvider) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleDomainEventPublisher = vehicleDomainEventPublisher;
        this.currentUserProvider = currentUserProvider;
    }

    public VehicleResponse addVehicle(Long customerId, VehicleRequest request) {
        Customer customer = findCustomerForCurrentUser(customerId);
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
            vehicleDomainEventPublisher.publish(VehicleEventType.CREATED, saved);
            return CustomerMapper.toVehicleResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
    }

    public VehicleResponse addVehicleForCurrentCustomer(VehicleRequest request) {
        Long customerId = resolveCurrentCustomerId();
        return addVehicle(customerId, request);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles(Long customerId) {
        findCustomerForCurrentUser(customerId);
        return vehicleRepository.findAllByCustomerId(customerId).stream()
                .sorted(VEHICLE_COMPARATOR)
                .map(CustomerMapper::toVehicleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesForCurrentCustomer() {
        Long customerId = resolveCurrentCustomerId();
        return getVehicles(customerId);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long customerId, Long vehicleId) {
        findCustomerForCurrentUser(customerId);
        Vehicle vehicle = vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        return CustomerMapper.toVehicleResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleForCurrentCustomer(Long vehicleId) {
        Long customerId = resolveCurrentCustomerId();
        return getVehicle(customerId, vehicleId);
    }

    public VehicleResponse updateVehicle(Long customerId, Long vehicleId, VehicleRequest request) {
        findCustomerForCurrentUser(customerId);
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
            vehicleDomainEventPublisher.publish(VehicleEventType.UPDATED, updated);
            return CustomerMapper.toVehicleResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
    }

    public VehicleResponse updateVehicleForCurrentCustomer(Long vehicleId, VehicleRequest request) {
        Long customerId = resolveCurrentCustomerId();
        return updateVehicle(customerId, vehicleId, request);
    }

    @Transactional(readOnly = true)
    public VehicleStatsResponse getVehicleStats(Long customerId) {
        findCustomerForCurrentUser(customerId);
        return buildVehicleStats(customerId);
    }

    @Transactional(readOnly = true)
    public VehicleStatsResponse getVehicleStatsForCurrentCustomer() {
        Long customerId = resolveCurrentCustomerId();
        return buildVehicleStats(customerId);
    }

    private VehicleStatsResponse buildVehicleStats(Long customerId) {
        long totalVehicles = vehicleRepository.countByCustomerId(customerId);
        return new VehicleStatsResponse(totalVehicles, Instant.now());
    }

    public void deleteVehicle(Long customerId, Long vehicleId) {
        findCustomerForCurrentUser(customerId);
        Vehicle vehicle = vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        vehicleDomainEventPublisher.publish(VehicleEventType.DELETED, vehicle);
        Customer customer = vehicle.getCustomer();
        if (customer != null) {
            customer.removeVehicle(vehicle);
        }
        vehicleRepository.delete(vehicle);
    }

    public void deleteVehicleForCurrentCustomer(Long vehicleId) {
        Long customerId = resolveCurrentCustomerId();
        deleteVehicle(customerId, vehicleId);
    }

    private Customer findCustomerForCurrentUser(Long customerId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (currentUser.hasRole("ADMIN")) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        }

        return customerRepository.findByIdAndEmailIgnoreCase(customerId, currentUser.normalizedEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for customer"));
    }

    private String normalizeLicensePlate(String licensePlate) {
        return licensePlate == null ? null : licensePlate.trim().toUpperCase();
    }

    private String normalizeVin(String vin) {
        return vin == null ? null : vin.trim().toUpperCase();
    }

    private Long resolveCurrentCustomerId() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return customerRepository.findByEmailIgnoreCase(currentUser.normalizedEmail())
                .map(Customer::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }

    /**
     * Public method to fetch vehicle details for cross-service consumption.
     * Used by other services (e.g., Project Service) to display vehicle information.
     * 
     * @param vehicleId the vehicle identifier
     * @return VehicleDetailsDto with essential fields (id, licensePlate, make, model, year)
     * @throws ResponseStatusException 404 if vehicle not found
     */
    @Transactional(readOnly = true)
    public com.autonova.customer.dto.VehicleDetailsDto getVehicleDetails(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        return CustomerMapper.toVehicleDetailsDto(vehicle);
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
