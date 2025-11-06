package com.autonova.employee_dashboard_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autonova.employee_dashboard_service.entity.EmployeePreferences;

@Repository
public interface EmployeePreferencesRepository extends JpaRepository<EmployeePreferences, Long> {
}
