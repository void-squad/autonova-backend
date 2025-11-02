package com.autonova.customer.dto;

import com.autonova.customer.model.Customer;
import com.autonova.customer.model.Vehicle;
import java.util.Comparator;
import java.util.List;

public final class CustomerMapper {

    private static final Comparator<Vehicle> VEHICLE_COMPARATOR =
        Comparator.comparing(Vehicle::getId, Comparator.nullsLast(Long::compare));

    private CustomerMapper() {
    }

    public static Customer toCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        customer.setPhoneNumber(request.phoneNumber().trim());
        return customer;
    }

    public static void updateCustomer(Customer customer, CustomerRequest request) {
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        customer.setPhoneNumber(request.phoneNumber().trim());
    }

    public static Vehicle toVehicle(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setMake(normalize(request.make()));
        vehicle.setModel(normalize(request.model()));
        vehicle.setYear(request.year());
        vehicle.setVin(request.vin().trim().toUpperCase());
        vehicle.setLicensePlate(request.licensePlate().trim().toUpperCase());
        return vehicle;
    }

    public static void updateVehicle(Vehicle vehicle, VehicleRequest request) {
        vehicle.setMake(normalize(request.make()));
        vehicle.setModel(normalize(request.model()));
        vehicle.setYear(request.year());
        vehicle.setVin(request.vin().trim().toUpperCase());
        vehicle.setLicensePlate(request.licensePlate().trim().toUpperCase());
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        List<VehicleResponse> vehicles = customer.getVehicles().stream()
                .sorted(VEHICLE_COMPARATOR)
                .map(CustomerMapper::toVehicleResponse)
                .toList();
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                vehicles,
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public static VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getCustomer() != null ? vehicle.getCustomer().getId() : null,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
