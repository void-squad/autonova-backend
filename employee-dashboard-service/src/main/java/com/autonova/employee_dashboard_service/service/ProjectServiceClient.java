package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.project.ProjectDto;
import com.autonova.employee_dashboard_service.dto.task.TaskDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

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
     * @param authorizationHeader Authorization header value to forward
     * @return List of projects
     */
    public Mono<List<ProjectDto>> getProjectsByAssignee(
            String assigneeId,
            boolean includeTasks,
            int page,
            int pageSize,
            String authorizationHeader
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
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
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
     * @param authorizationHeader Authorization header value to forward
     * @return Task list response with pagination
     */
    public Mono<TaskListResponse> getTasksByAssignee(
            String assigneeId,
            String status,
            int page,
            int pageSize,
            String authorizationHeader
    ) {
        log.info("Fetching assigned tasks via gateway for assignee hint: {} with status: {}", assigneeId, status);

        int resolvedPage = page < 1 ? 1 : page;
        int resolvedPageSize = pageSize < 1 ? 20 : pageSize;
        String normalizedStatus = status != null ? status.toLowerCase(Locale.ROOT) : null;

        return webClientBuilder.build()
                .get()
                .uri(gatewayUrl + "/api/tasks/assigned")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .bodyToFlux(TaskDto.class)
                .collectList()
                .map(tasks -> filterAndPaginateTasks(tasks, normalizedStatus, resolvedPage, resolvedPageSize))
                .doOnSuccess(response -> log.info("Successfully fetched {} tasks after filtering", response.getTotal()))
                .doOnError(error -> log.error("Error fetching assigned tasks: {}", error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch assigned tasks for assignee {}: {}", assigneeId, error.getMessage());
                    return Mono.just(TaskListResponse.builder()
                            .page(resolvedPage)
                            .pageSize(resolvedPageSize)
                            .total(0)
                            .items(List.of())
                            .build());
                });
    }

    private TaskListResponse filterAndPaginateTasks(List<TaskDto> allTasks, String normalizedStatus, int page, int pageSize) {
        List<TaskDto> filtered = allTasks;
        if (normalizedStatus != null && !normalizedStatus.isBlank()) {
            filtered = allTasks.stream()
                    .filter(task -> task.getStatus() != null && task.getStatus().toLowerCase(Locale.ROOT).equals(normalizedStatus))
                    .toList();
        }

        int total = filtered.size();
        int fromIndex = Math.min(Math.max((page - 1) * pageSize, 0), total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TaskDto> paged = filtered.subList(fromIndex, toIndex);

        return TaskListResponse.builder()
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .items(paged)
                .build();
    }
}
