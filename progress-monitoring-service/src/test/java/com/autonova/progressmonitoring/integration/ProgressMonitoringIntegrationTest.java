package com.autonova.progressmonitoring.integration;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.entity.ProjectMessage;
import com.autonova.progressmonitoring.repository.ProjectMessageRepository;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Progress Monitoring Service with RabbitMQ and PostgreSQL using Testcontainers
 */
@SpringBootTest
@Testcontainers
class ProgressMonitoringIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("progressdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // RabbitMQ properties
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired
    private ProjectMessageService projectMessageService;

    @Autowired
    private ProjectMessageRepository projectMessageRepository;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectMessageRepository.deleteAll();
        projectId = UUID.randomUUID();
    }

    @Test
    void testContainersAreRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    void saveProjectMessage_withValidData_savesToDatabase() {
        // Given
        ProjectMessageDto messageDto = ProjectMessageDto.builder()
                .projectId(projectId)
                .category("UPDATE")
                .message("Project status updated")
                .payload("status:IN_PROGRESS")
                .occurredAt(OffsetDateTime.now())
                .build();

        // When
        ProjectMessageDto saved = projectMessageService.saveMessage(messageDto);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getMessage()).isEqualTo("Project status updated");

        // Verify in database
        List<ProjectMessage> messagesInDb = projectMessageRepository.findAll();
        assertThat(messagesInDb).hasSize(1);
        assertThat(messagesInDb.get(0).getProjectId()).isEqualTo(projectId);
    }

    @Test
    void getMessagesForProject_returnsAllMessagesForProject() {
        // Given
        ProjectMessageDto message1 = ProjectMessageDto.builder()
                .projectId(projectId)
                .category("CREATED")
                .message("Project created")
                .occurredAt(OffsetDateTime.now())
                .build();
        
        ProjectMessageDto message2 = ProjectMessageDto.builder()
                .projectId(projectId)
                .category("UPDATE")
                .message("Project updated")
                .occurredAt(OffsetDateTime.now().plusHours(1))
                .build();

        projectMessageService.saveMessage(message1);
        projectMessageService.saveMessage(message2);

        // When
        List<ProjectMessageDto> messages = projectMessageService.getMessagesForProjectDto(projectId);

        // Then
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(ProjectMessageDto::getMessage)
                .containsExactlyInAnyOrder("Project created", "Project updated");
    }

    @Test
    void saveMultipleMessagesForDifferentProjects_isolatesCorrectly() {
        // Given
        UUID project1 = UUID.randomUUID();
        UUID project2 = UUID.randomUUID();

        ProjectMessageDto message1 = ProjectMessageDto.builder()
                .projectId(project1)
                .category("UPDATE")
                .message("Project 1 message")
                .occurredAt(OffsetDateTime.now())
                .build();
        
        ProjectMessageDto message2 = ProjectMessageDto.builder()
                .projectId(project2)
                .category("UPDATE")
                .message("Project 2 message")
                .occurredAt(OffsetDateTime.now())
                .build();

        projectMessageService.saveMessage(message1);
        projectMessageService.saveMessage(message2);

        // When
        List<ProjectMessageDto> project1Messages = projectMessageService.getMessagesForProjectDto(project1);
        List<ProjectMessageDto> project2Messages = projectMessageService.getMessagesForProjectDto(project2);

        // Then
        assertThat(project1Messages).hasSize(1);
        assertThat(project2Messages).hasSize(1);
        assertThat(project1Messages.get(0).getMessage()).isEqualTo("Project 1 message");
        assertThat(project2Messages.get(0).getMessage()).isEqualTo("Project 2 message");
    }

    @Test
    void saveMessagesAndRetrievePaginated_returnsCorrectPage() {
        // Given - Save 5 messages
        for (int i = 1; i <= 5; i++) {
            ProjectMessageDto message = ProjectMessageDto.builder()
                    .projectId(projectId)
                    .category("UPDATE")
                    .message("Message " + i)
                    .occurredAt(OffsetDateTime.now().plusMinutes(i))
                    .build();
            projectMessageService.saveMessage(message);
        }

        // When - Retrieve first page with 3 messages
        var page = projectMessageService.getMessagesPage(projectId, 0, 3);

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.hasNext()).isTrue();
    }
}
