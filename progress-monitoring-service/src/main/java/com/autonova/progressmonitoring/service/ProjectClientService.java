package com.autonova.progressmonitoring.service;

import com.autonova.progressmonitoring.client.ProjectServiceClient;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.client.ProjectServiceClient.ProjectServiceException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProjectClientService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectClientService.class);

    private final ProjectServiceClient projectClient;

    public ProjectClientService(ProjectServiceClient projectClient) {
        this.projectClient = projectClient;
    }

    public void enrichMessagesWithProjectTitle(String projectId, List<ProjectMessageDto> messages) {
        if (messages == null || messages.isEmpty()) return;

        projectClient.getProjectById(projectId)
                .doOnTerminate(() -> logger.info("Enrichment for project {} completed", projectId))
                .subscribe(proj -> applyTitleIfMissing(messages, proj),
                        error -> handleProjectServiceError(projectId, error));
    }

    private void applyTitleIfMissing(List<ProjectMessageDto> messages, Map<String, Object> proj) {
        if (messages.isEmpty()) return;

        ProjectMessageDto first = messages.get(0);
        if (first.getPayload() == null || first.getPayload().isBlank()) {
            String title = proj != null ? String.valueOf(proj.getOrDefault("title", "")) : "";
            first.setPayload("project_title:" + title);
        }
    }

    private void handleProjectServiceError(String projectId, Throwable error) {
        if (error instanceof ProjectServiceException) {
            ProjectServiceException projectError = (ProjectServiceException) error;
            logger.error("Error occurred while fetching project with ID {}. Status: {}, Message: {}", 
                         projectId, projectError.getStatusCode(), projectError.getMessage());
        } else {
            logger.error("Unexpected error occurred while fetching project with ID {}: {}", projectId, error.getMessage());
        }
    }
}
