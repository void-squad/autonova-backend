package com.autonova.analytics.repository;

import com.autonova.analytics.entity.EmployeePerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeePerformanceRepository extends JpaRepository<EmployeePerformance, Long> {

    @Query("SELECT e FROM EmployeePerformance e ORDER BY e.efficiency DESC")
    List<EmployeePerformance> findTopPerformers();
}