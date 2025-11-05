package com.autonova.employee_dashboard_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePreferences {

    @Id
    @Column(name = "employee_id")
    private Long employeeId; // Foreign Key to Authentication service's user table

    @Column(name = "default_view", nullable = false)
    @Enumerated(EnumType.STRING)
    private ViewType defaultView;

    @Column(name = "theme", nullable = false)
    @Enumerated(EnumType.STRING)
    private Theme theme;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ViewType {
        OPERATIONAL,
        ANALYTICAL
    }

    public enum Theme {
        DARK,
        LIGHT
    }
}
