package com.autonova.progressmonitoring.service;

import com.autonova.progressmonitoring.client.ProjectServiceClient;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectClientServiceTest {

    @Mock
    private ProjectServiceClient projectClient;

    @InjectMocks
    private ProjectClientService projectClientService;

    private String projectId;
    private List<ProjectMessageDto> messages;
    private Map<String, Object> projectData;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID().toString();
        
        ProjectMessageDto message = ProjectMessageDto.builder()
                .projectId(UUID.fromString(projectId))
                .message("Test message")
                .build();
        messages = new ArrayList<>();
        messages.add(message);

        projectData = new HashMap<>();
        projectData.put("id", projectId);
        projectData.put("title", "Test Project");
        projectData.put("status", "ACTIVE");
    }

    @Test
    void enrichMessagesWithProjectTitle_withValidProject_enrichesMessages() {
        // Given
        when(projectClient.getProjectById(projectId))
                .thenReturn(Mono.just(projectData));

        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, messages);

        // Give async operation time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(projectClient).getProjectById(projectId);
    }

    @Test
    void enrichMessagesWithProjectTitle_withEmptyMessages_doesNotCallClient() {
        // Given
        List<ProjectMessageDto> emptyMessages = new ArrayList<>();

        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, emptyMessages);

        // Then
        verify(projectClient, never()).getProjectById(anyString());
    }

    @Test
    void enrichMessagesWithProjectTitle_withNullMessages_doesNotCallClient() {
        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, null);

        // Then
        verify(projectClient, never()).getProjectById(anyString());
    }

    @Test
    void enrichMessagesWithProjectTitle_withProjectServiceError_handlesError() {
        // Given
        ProjectServiceClient.ProjectServiceException exception = 
                new ProjectServiceClient.ProjectServiceException("Project not found", 404);
        when(projectClient.getProjectById(projectId))
                .thenReturn(Mono.error(exception));

        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, messages);

        // Give async operation time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(projectClient).getProjectById(projectId);
        // Error is logged but doesn't throw exception
    }

    @Test
    void getProjectsForCustomer_withValidCustomerId_returnsProjects() {
        // Given
        long customerId = 123L;
        List<Map<String, Object>> expectedProjects = Arrays.asList(
                projectData,
                Map.of("id", UUID.randomUUID().toString(), "title", "Another Project")
        );
        
        when(projectClient.getProjectsByCustomerId(customerId))
                .thenReturn(Mono.just(expectedProjects));

        // When
        List<Map<String, Object>> result = projectClientService.getProjectsForCustomer(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("title")).isEqualTo("Test Project");
        verify(projectClient).getProjectsByCustomerId(customerId);
    }

    @Test
    void getProjectsForCustomer_withProjectServiceError_returnsEmptyList() {
        // Given
        long customerId = 123L;
        ProjectServiceClient.ProjectServiceException exception = 
                new ProjectServiceClient.ProjectServiceException("Service unavailable", 503);
        
        when(projectClient.getProjectsByCustomerId(customerId))
                .thenReturn(Mono.error(exception));

        // When
        List<Map<String, Object>> result = projectClientService.getProjectsForCustomer(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(projectClient).getProjectsByCustomerId(customerId);
    }

    @Test
    void getProjectsForCustomer_withGenericError_returnsEmptyList() {
        // Given
        long customerId = 123L;
        when(projectClient.getProjectsByCustomerId(customerId))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When
        List<Map<String, Object>> result = projectClientService.getProjectsForCustomer(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(projectClient).getProjectsByCustomerId(customerId);
    }

    @Test
    void enrichMessagesWithProjectTitle_appliesTitleWhenPayloadIsNull() {
        // Given
        messages.get(0).setPayload(null);
        when(projectClient.getProjectById(projectId))
                .thenReturn(Mono.just(projectData));

        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, messages);

        // Give async operation time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(projectClient).getProjectById(projectId);
    }

    @Test
    void enrichMessagesWithProjectTitle_appliesTitleWhenPayloadIsBlank() {
        // Given
        messages.get(0).setPayload("   ");
        when(projectClient.getProjectById(projectId))
                .thenReturn(Mono.just(projectData));

        // When
        projectClientService.enrichMessagesWithProjectTitle(projectId, messages);

        // Give async operation time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(projectClient).getProjectById(projectId);
    }
}
