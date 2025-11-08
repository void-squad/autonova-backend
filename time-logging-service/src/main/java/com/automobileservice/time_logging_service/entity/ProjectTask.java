package com.automobileservice.time_logging_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTask {
    @Id
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;
    
    @Column(name = "estimated_hours", precision = 10, scale = 2)
    private BigDecimal estimatedHours;
    
    @Column(name = "actual_hours", precision = 10, scale = 2)
    private BigDecimal actualHours;
    
    @Column(nullable = false, length = 50)
    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED
    
    @Column(length = 20)
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}