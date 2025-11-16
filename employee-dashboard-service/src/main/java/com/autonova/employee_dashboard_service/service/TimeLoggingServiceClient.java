package com.autonova.employee_dashboard_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.autonova.employee_dashboard_service.dto.timelog.TimeLogDto;
import com.autonova.employee_dashboard_service.dto.timelog.TimeLogListResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeLoggingServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.gateway.url}")
    private String gatewayUrl;

    public Mono<TimeLogListResponse> getTimeLogsByEmployee(
            String employeeId,
            int page,
            int pageSize,
            String authorizationHeader
    ) {
        log.info("Fetching time logs for employee {}", employeeId);

        int resolvedPage = page < 1 ? 1 : page;
        int resolvedPageSize = pageSize < 1 ? 20 : pageSize;

        WebClient.RequestHeadersSpec<?> requestSpec = webClientBuilder.build()
                .get()
                .uri(gatewayUrl + "/api/time-logs/employee/{employeeId}", employeeId);

        if (StringUtils.hasText(authorizationHeader)) {
            requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        return requestSpec
                .retrieve()
                .bodyToFlux(TimeLogDto.class)
                .collectList()
                .map(timeLogs -> buildResponse(timeLogs, resolvedPage, resolvedPageSize))
                .doOnSuccess(response -> log.info("Fetched {} time logs for employee {}", response.getTotal(), employeeId))
                .doOnError(error -> log.error("Error fetching time logs for employee {}: {}", employeeId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch time logs for employee {}: {}", employeeId, error.getMessage());
                    return Mono.just(TimeLogListResponse.builder()
                            .page(resolvedPage)
                            .pageSize(resolvedPageSize)
                            .total(0)
                            .items(List.of())
                            .build());
                });
    }

    private TimeLogListResponse buildResponse(List<TimeLogDto> timeLogs, int page, int pageSize) {
        List<TimeLogDto> safeLogs = timeLogs != null ? timeLogs : List.of();
        int total = safeLogs.size();
        int fromIndex = Math.min(Math.max((page - 1) * pageSize, 0), total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TimeLogDto> pagedItems = safeLogs.subList(fromIndex, toIndex);

        return TimeLogListResponse.builder()
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .items(pagedItems)
                .build();
    }
}
