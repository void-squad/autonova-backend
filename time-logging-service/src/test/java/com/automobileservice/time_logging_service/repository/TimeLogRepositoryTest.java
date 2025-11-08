package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
class TimeLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TimeLogRepository timeLogRepository;

    private Employee employee;
    private Project project;
    private ProjectTask task;
    private TimeLog pendingLog;
    private TimeLog approvedLog;
    private TimeLog rejectedLog;

    @BeforeEach
    void setUp() {
        // Create test user
        User user = new User();
        user.setId("emp-001");
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("EMPLOYEE");
        entityManager.persist(user);

        // Create test employee
        employee = new Employee();
        employee.setUserId("emp-001");
        employee.setUser(user);
        employee.setEmployeeCode("EMP001");
        entityManager.persist(employee);

        // Create test project
        project = new Project();
        project.setId("proj-001");
        project.setTitle("Test Project");
        project.setStatus("IN_PROGRESS");
        entityManager.persist(project);

        // Create test task
        task = new ProjectTask();
        task.setId("task-001");
        task.setProject(project);
        task.setTaskName("Test Task");
        task.setAssignedEmployee(employee);
        task.setEstimatedHours(BigDecimal.valueOf(5.0));
        entityManager.persist(task);

        // Create test time logs with different statuses
        LocalDateTime now = LocalDateTime.now();

        pendingLog = new TimeLog();
        pendingLog.setId("log-pending");
        pendingLog.setEmployee(employee);
        pendingLog.setProject(project);
        pendingLog.setTask(task);
        pendingLog.setHours(BigDecimal.valueOf(2.5));
        pendingLog.setApprovalStatus("PENDING");
        pendingLog.setLoggedAt(now.minusHours(2));
        entityManager.persist(pendingLog);

        approvedLog = new TimeLog();
        approvedLog.setId("log-approved");
        approvedLog.setEmployee(employee);
        approvedLog.setProject(project);
        approvedLog.setTask(task);
        approvedLog.setHours(BigDecimal.valueOf(3.0));
        approvedLog.setApprovalStatus("APPROVED");
        approvedLog.setLoggedAt(now.minusHours(1));
        entityManager.persist(approvedLog);

        rejectedLog = new TimeLog();
        rejectedLog.setId("log-rejected");
        rejectedLog.setEmployee(employee);
        rejectedLog.setProject(project);
        rejectedLog.setTask(task);
        rejectedLog.setHours(BigDecimal.valueOf(1.5));
        rejectedLog.setApprovalStatus("REJECTED");
        rejectedLog.setLoggedAt(now);
        entityManager.persist(rejectedLog);

        entityManager.flush();
    }

    @Test
    void findAllByOrderByLoggedAtDesc_ReturnsLogsOrderedByDate() {
        // Act
        List<TimeLog> logs = timeLogRepository.findAllByOrderByLoggedAtDesc();

        // Assert
        assertThat(logs).hasSize(3);
        // Should be ordered by loggedAt DESC (most recent first)
        assertThat(logs.get(0).getId()).isEqualTo("log-rejected");
        assertThat(logs.get(1).getId()).isEqualTo("log-approved");
        assertThat(logs.get(2).getId()).isEqualTo("log-pending");
    }

    @Test
    void findByApprovalStatusOrderByLoggedAtDesc_ReturnsFilteredAndOrderedLogs() {
        // Act
        List<TimeLog> pendingLogs = timeLogRepository.findByApprovalStatusOrderByLoggedAtDesc("PENDING");

        // Assert
        assertThat(pendingLogs).hasSize(1);
        assertThat(pendingLogs.get(0).getId()).isEqualTo("log-pending");
        assertThat(pendingLogs.get(0).getApprovalStatus()).isEqualTo("PENDING");
    }

    @Test
    void getTotalApprovedHoursByEmployee_CalculatesOnlyApprovedLogs() {
        // Act
        BigDecimal totalHours = timeLogRepository.getTotalApprovedHoursByEmployee("emp-001");

        // Assert
        assertThat(totalHours).isEqualTo(BigDecimal.valueOf(3.0)); // Only approved log
    }

    @Test
    void getTotalApprovedHoursByTask_CalculatesOnlyApprovedLogs() {
        // Act
        BigDecimal totalHours = timeLogRepository.getTotalApprovedHoursByTask("task-001");

        // Assert
        assertThat(totalHours).isEqualTo(BigDecimal.valueOf(3.0)); // Only approved log
    }

    @Test
    void findByEmployeeUserIdOrderByLoggedAtDesc_ReturnsEmployeeLogsOrdered() {
        // Act
        List<TimeLog> logs = timeLogRepository.findByEmployeeUserIdOrderByLoggedAtDesc("emp-001");

        // Assert
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getId()).isEqualTo("log-rejected");
        assertThat(logs.get(1).getId()).isEqualTo("log-approved");
        assertThat(logs.get(2).getId()).isEqualTo("log-pending");
    }

    @Test
    void findByEmployeeUserIdAndApprovalStatusOrderByLoggedAtDesc_ReturnsFilteredLogs() {
        // Act
        List<TimeLog> approvedLogs = timeLogRepository
            .findByEmployeeUserIdAndApprovalStatusOrderByLoggedAtDesc("emp-001", "APPROVED");

        // Assert
        assertThat(approvedLogs).hasSize(1);
        assertThat(approvedLogs.get(0).getId()).isEqualTo("log-approved");
    }

    @Test
    void findApprovedByEmployeeUserIdAndLoggedAtAfter_ReturnsApprovedLogsAfterDate() {
        // Arrange
        LocalDateTime after = LocalDateTime.now().minusHours(1).minusMinutes(30);

        // Act
        List<TimeLog> logs = timeLogRepository
            .findApprovedByEmployeeUserIdAndLoggedAtAfter("emp-001", after);

        // Assert
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getId()).isEqualTo("log-approved");
    }

    @Test
    void findApprovedByEmployeeUserIdAndLoggedAtBetween_ReturnsApprovedLogsInRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // Act
        List<TimeLog> logs = timeLogRepository
            .findApprovedByEmployeeUserIdAndLoggedAtBetween("emp-001", start, end);

        // Assert
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getId()).isEqualTo("log-approved");
    }

    @Test
    void getTotalHoursByEmployee_IncludesAllStatuses() {
        // Act
        BigDecimal totalHours = timeLogRepository.getTotalHoursByEmployee("emp-001");

        // Assert
        assertThat(totalHours).isEqualTo(BigDecimal.valueOf(7.0)); // 2.5 + 3.0 + 1.5
    }

    @Test
    void findByProjectIdOrderByLoggedAtDesc_ReturnsProjectLogs() {
        // Act
        List<TimeLog> logs = timeLogRepository.findByProjectIdOrderByLoggedAtDesc("proj-001");

        // Assert
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getId()).isEqualTo("log-rejected");
    }

    @Test
    void findByTaskIdOrderByLoggedAtDesc_ReturnsTaskLogs() {
        // Act
        List<TimeLog> logs = timeLogRepository.findByTaskIdOrderByLoggedAtDesc("task-001");

        // Assert
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getId()).isEqualTo("log-rejected");
    }
}