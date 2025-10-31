package com.automobileservice.time_logging_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(nullable = false, length = 50)
    private String make;
    
    @Column(nullable = false, length = 50)
    private String model;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(name = "license_plate", unique = true, nullable = false, length = 20)
    private String licensePlate;
    
    @Column(unique = true, nullable = false, length = 17)
    private String vin;
    
    @Column(length = 30)
    private String color;
}