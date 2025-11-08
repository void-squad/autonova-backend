package com.autonova.notification.sse;

import com.autonova.notification.dto.NotificationDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SseHub {
    private final Map<Long, Sinks.Many<NotificationDto>> userSinks = new ConcurrentHashMap<>();

    public Flux<NotificationDto> subscribe(Long userId) {
        var sink = userSinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer(64, false));
        // Merge with heartbeat to keep connection alive
        Flux<NotificationDto> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> new NotificationDto(
                        null,
                        userId,
                        "heartbeat",
                        "heartbeat",
                        "Heartbeat",
                        "keepalive",
                        null,
                        Instant.now(),
                        true
                ));
        return Flux.merge(sink.asFlux(), heartbeat);
    }

    public void publish(NotificationDto dto) {
        var sink = userSinks.computeIfAbsent(dto.userId(), id -> Sinks.many().multicast().onBackpressureBuffer(64, false));
        sink.tryEmitNext(dto);
    }
}
