package com.autonova.progressmonitoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import org.springframework.http.HttpStatusCode;

@Component
public class ProjectServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceClient.class);
    private final WebClient webClient;

    public ProjectServiceClient(@Value("${PROJECT_SERVICE_URL:http://localhost:8082}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public Mono<Map<String, Object>> getProjectById(String projectId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/projects/{id}").build(projectId))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), 
                        response -> {
                            logger.error("Client error while fetching project {}: Status code {}", projectId, response.statusCode());
                            return Mono.error(new ProjectServiceException("Client error while fetching project", response.statusCode().value()));
                        })
                .onStatus(status -> status.is5xxServerError(), 
                        response -> {
                            logger.error("Server error while fetching project {}: Status code {}", projectId, response.statusCode());
                            return Mono.error(new ProjectServiceException("Server error while fetching project", response.statusCode().value()));
                        })
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorMap(e -> {
                    logger.error("Error occurred while fetching project with ID: {}", projectId, e);
                    return new ProjectServiceException("Unexpected error occurred", e);
                })
                .doOnTerminate(() -> logger.info("Request to fetch project {} completed", projectId));
    }

    public Mono<java.util.List<Map<String, Object>>> getProjectsByCustomerId(long customerId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/projects").queryParam("customerId", String.valueOf(customerId)).build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> {
                            logger.error("Client error while fetching projects for customer {}: Status code {}", customerId, response.statusCode());
                            return Mono.error(new ProjectServiceException("Client error while fetching projects", response.statusCode().value()));
                        })
                .onStatus(status -> status.is5xxServerError(),
                        response -> {
                            logger.error("Server error while fetching projects for customer {}: Status code {}", customerId, response.statusCode());
                            return Mono.error(new ProjectServiceException("Server error while fetching projects", response.statusCode().value()));
                        })
                .bodyToMono(new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {})
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorMap(e -> {
                    logger.error("Error occurred while fetching projects for customer {}: {}", customerId, e.getMessage());
                    return new ProjectServiceException("Unexpected error occurred", e);
                })
                .doOnTerminate(() -> logger.info("Request to fetch projects for customer {} completed", customerId));
    }

    public static class ProjectServiceException extends RuntimeException {
        private final int statusCode;

        public ProjectServiceException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public ProjectServiceException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1; // Unknown status code
        }

        public ProjectServiceException(String message, HttpStatusCode status) {
            this(message, status.value());
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
