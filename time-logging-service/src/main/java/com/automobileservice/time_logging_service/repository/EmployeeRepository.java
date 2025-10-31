package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    // Find employee by employee code
    Optional<Employee> findByEmployeeCode(String employeeCode);
    
    // Find employees by department
    java.util.List<Employee> findByDepartment(String department);
    
    // Find employee by user email
    Optional<Employee> findByUserEmail(String email);
}