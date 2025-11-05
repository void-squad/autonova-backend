package com.autonova.employee_dashboard_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autonova.employee_dashboard_service.entity.SavedAnalyticsReport;

@Repository
public interface SavedAnalyticsReportRepository extends JpaRepository<SavedAnalyticsReport, Long> {
    
    List<SavedAnalyticsReport> findByEmployeeId(Long employeeId);
}
