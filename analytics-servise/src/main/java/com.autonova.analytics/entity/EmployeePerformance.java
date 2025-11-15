package com.autonova.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_performance")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private Integer tasksCompleted;

    @Column(nullable = false)
    private Integer hoursLogged;

    @Column(nullable = false)
    private Integer efficiency;
}