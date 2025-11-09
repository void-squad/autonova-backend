package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.project.ProjectDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Client service to interact with Project Service through the Gateway
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.gateway.url}")
    private String gatewayUrl;

    /**
     * Get projects assigned to a specific user
     * 
     * @param assigneeId User ID (UUID as string)
     * @param includeTasks Whether to include tasks in the response
     * @param page Page number
     * @param pageSize Number of items per page
     * @param token JWT token for authentication
     * @return List of projects
     */
    public Mono<List<ProjectDto>> getProjectsByAssignee(
            String assigneeId,
            boolean includeTasks,
            int page,
            int pageSize,
            String token
    ) {
        log.info("Fetching projects for assignee: {}", assigneeId);

        return webClientBuilder.build()
                .get()
                .uri(gatewayUrl + "/api/projects",
                        uriBuilder -> uriBuilder
                                .queryParam("assigneeId", assigneeId)
                                .queryParam("includeTasks", includeTasks)
                                .queryParam("page", page)
                                .queryParam("pageSize", pageSize)
                                .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProjectDto>>() {})
                .doOnSuccess(projects -> log.info("Successfully fetched {} projects", projects != null ? projects.size() : 0))
                .doOnError(error -> log.error("Error fetching projects: {}", error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch projects for assignee {}: {}", assigneeId, error.getMessage());
                    return Mono.just(List.of());
                });
    }

    /**
     * Get tasks assigned to a specific user
     * 
     * @param assigneeId User ID (UUID as string)
     * @param status Task status filter (optional)
     * @param page Page number
     * @param pageSize Number of items per page
     * @param token JWT token for authentication
     * @return Task list response with pagination
     */
    public Mono<TaskListResponse> getTasksByAssignee(
            String assigneeId,
            String status,
            int page,
            int pageSize,
            String token
    ) {
        log.info("Fetching tasks for assignee: {} with status: {}", assigneeId, status);

        WebClient.RequestHeadersUriSpec<?> requestSpec = webClientBuilder.build()
                .get();

        return requestSpec
                .uri(gatewayUrl + "/api/tasks",
                        uriBuilder -> {
                            uriBuilder.queryParam("assigneeId", assigneeId)
                                    .queryParam("page", page)
                                    .queryParam("pageSize", pageSize);
                            
                            if (status != null && !status.isEmpty()) {
                                uriBuilder.queryParam("status", status);
                            }
                            
                            return uriBuilder.build();
                        })
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(TaskListResponse.class)
                .doOnSuccess(response -> log.info("Successfully fetched {} tasks", response != null ? response.getTotal() : 0))
                .doOnError(error -> log.error("Error fetching tasks: {}", error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch tasks for assignee {}: {}", assigneeId, error.getMessage());
                    return Mono.just(TaskListResponse.builder()
                            .page(page)
                            .pageSize(pageSize)
                            .total(0)
                            .items(List.of())
                            .build());
                });
    }
}
