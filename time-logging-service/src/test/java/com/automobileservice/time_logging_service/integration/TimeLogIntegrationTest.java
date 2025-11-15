package com.automobileservice.time_logging_service.integration;

import com.automobileservice.time_logging_service.entity.*;
import com.automobileservice.time_logging_service.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
@Transactional
class TimeLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    private String employeeId;
    private String projectId;
    private String taskId;

    @BeforeEach
    void setUp() {
        // Create test data
        User user = new User();
        user.setId("emp-int-001");
        user.setFirstName("Integration");
        user.setLastName("Test");
        user.setEmail("integration@test.com");
        user.setRole("EMPLOYEE");

        Employee employee = new Employee();
        employee.setUserId("emp-int-001");
        employee.setUser(user);
        employee.setEmployeeCode("INT001");

        employeeRepository.save(employee);
        employeeId = employee.getUserId();

        Project project = new Project();
        project.setId("proj-int-001");
        project.setTitle("Integration Test Project");
        project.setStatus("IN_PROGRESS");

        projectRepository.save(project);
        projectId = project.getId();

        ProjectTask task = new ProjectTask();
        task.setId("task-int-001");
        task.setProject(project);
        task.setTaskName("Integration Test Task");
        task.setAssignedEmployee(employee);
        task.setEstimatedHours(BigDecimal.valueOf(10.0));

        projectTaskRepository.save(task);
        taskId = task.getId();
    }

    @Test
    void fullTimeLogWorkflow_Success() throws Exception {
        // 1. Create time log
        String createRequest = String.format("""
            {
                "employeeId": "%s",
                "projectId": "%s",
                "taskId": "%s",
                "hours": 3.5,
                "note": "Integration test work"
            }
            """, employeeId, projectId, taskId);

        String responseJson = mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.approvalStatus", is("PENDING")))
                .andExpect(jsonPath("$.hours", is(3.5)))
                .andReturn().getResponse().getContentAsString();

        String timeLogId = objectMapper.readTree(responseJson).get("id").asText();

        // 2. Verify time log was created
        mockMvc.perform(get("/api/time-logs/" + timeLogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(timeLogId)))
                .andExpect(jsonPath("$.approvalStatus", is("PENDING")));

        // 3. Check it appears in pending logs
        mockMvc.perform(get("/api/time-logs/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.id == '" + timeLogId + "')].approvalStatus", contains("PENDING")));

        // 4. Approve the time log
        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(timeLogId)))
                .andExpect(jsonPath("$.approvalStatus", is("APPROVED")));

        // 5. Verify approval status changed
        mockMvc.perform(get("/api/time-logs/" + timeLogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus", is("APPROVED")));

        // 6. Check total hours calculation includes approved log
        mockMvc.perform(get("/api/time-logs/employee/" + employeeId + "/total-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(3.5)));

        // 7. Reject the approved log (demonstrate rejection of approved logs)
        String rejectRequest = """
            {
                "reason": "Incorrect task assignment"
            }
            """;

        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus", is("REJECTED")))
                .andExpect(jsonPath("$.note", containsString("REJECTION REASON: Incorrect task assignment")));

        // 8. Verify total hours no longer includes rejected log
        mockMvc.perform(get("/api/time-logs/employee/" + employeeId + "/total-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0.0)));
    }

    @Test
    void timeLogValidationRules_Enforced() throws Exception {
        // Test: Cannot log time on non-existent employee
        String invalidRequest = """
            {
                "employeeId": "non-existent-emp",
                "projectId": "%s",
                "taskId": "%s",
                "hours": 2.0
            }
            """.formatted(projectId, taskId);

        mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Employee")));

        // Test: Cannot log time on completed project
        Project project = projectRepository.findById(projectId).orElseThrow();
        project.setStatus("COMPLETED");
        projectRepository.save(project);

        String validRequest = String.format("""
            {
                "employeeId": "%s",
                "projectId": "%s",
                "taskId": "%s",
                "hours": 2.0
            }
            """, employeeId, projectId, taskId);

        mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("completed or cancelled project")));
    }

    @Test
    void approvalWorkflow_BusinessRules_Enforced() throws Exception {
        // Create a time log first
        String createRequest = String.format("""
            {
                "employeeId": "%s",
                "projectId": "%s",
                "taskId": "%s",
                "hours": 4.0
            }
            """, employeeId, projectId, taskId);

        String responseJson = mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String timeLogId = objectMapper.readTree(responseJson).get("id").asText();

        // Test: Cannot approve already approved log
        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already approved")));

        // Test: Cannot reject already rejected log
        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Test rejection\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/time-logs/" + timeLogId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Another rejection\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already rejected")));
    }

    @Test
    void employeeTimeLogQueries_WorkCorrectly() throws Exception {
        // Create multiple time logs for the employee
        String[] statuses = {"PENDING", "APPROVED", "REJECTED"};

        for (int i = 0; i < 3; i++) {
            String createRequest = String.format("""
                {
                    "employeeId": "%s",
                    "projectId": "%s",
                    "taskId": "%s",
                    "hours": %.1f
                }
                """, employeeId, projectId, taskId, (i + 1) * 2.0);

            mockMvc.perform(post("/api/time-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
                    .andExpect(status().isCreated());

            // Manually set status for testing (in real scenario, admin would do this)
            TimeLog log = timeLogRepository.findAll().get(i);
            log.setApprovalStatus(statuses[i]);
            timeLogRepository.save(log);
        }

        // Test: Get all employee logs
        mockMvc.perform(get("/api/time-logs/employee/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Test: Total hours calculation (only approved)
        mockMvc.perform(get("/api/time-logs/employee/" + employeeId + "/total-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(4.0))); // Only the approved log (2.0 * 2)
    }

    @Test
    void adminEndpoints_WorkCorrectly() throws Exception {
        // Create some time logs with different statuses
        for (int i = 0; i < 5; i++) {
            String createRequest = String.format("""
                {
                    "employeeId": "%s",
                    "projectId": "%s",
                    "taskId": "%s",
                    "hours": %.1f
                }
                """, employeeId, projectId, taskId, i + 1.0);

            mockMvc.perform(post("/api/time-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
                    .andExpect(status().isCreated());
        }

        // Test: Get all time logs (admin view)
        mockMvc.perform(get("/api/time-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        // Test: Get pending logs
        mockMvc.perform(get("/api/time-logs/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5))); // All are pending initially

        // Approve one log
        TimeLog firstLog = timeLogRepository.findAll().get(0);
        mockMvc.perform(patch("/api/time-logs/" + firstLog.getId() + "/approve"))
                .andExpect(status().isOk());

        // Verify pending count decreased
        mockMvc.perform(get("/api/time-logs/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }
}