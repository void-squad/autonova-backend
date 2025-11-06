package com.autonova.progressmonitoring.sse;

import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry implements EventPublisher {
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String projectId, SseEmitter emitter) {
        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }

    public void remove(String projectId, SseEmitter emitter) {
        var list = emitters.get(projectId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(projectId);
            }
        }
    }

    public void sendToProject(String projectId, String eventJson) {
        var list = emitters.get(projectId);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("project.update").data(eventJson));
            } catch (Exception e) {
                // IOException, IllegalStateException, or other send issues -> remove emitter
                remove(projectId, emitter);
            }
        }
    }

    public void sendMessageToProject(String projectId, String message) {
        var list = emitters.get(projectId);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("project.message").data(message));
            } catch (Exception e) {
                remove(projectId, emitter);
            }
        }
    }

    public void broadcastToAll(String eventJson) {
        // snapshot keys to avoid concurrent modification while iterating
        for (String key : List.copyOf(emitters.keySet())) {
            sendToProject(key, eventJson);
        }
    }

    public void broadcastMessageToAll(String message) {
        for (String key : List.copyOf(emitters.keySet())) {
            sendMessageToProject(key, message);
        }
    }

    @Override
    public void publishToProject(String projectId, String payload) {
        sendToProject(projectId, payload);
    }

    @Override
    public void broadcast(String payload) {
        broadcastToAll(payload);
    }

    @Override
    public void publishMessageToProject(String projectId, String message) {
        sendMessageToProject(projectId, message);
    }

    @Override
    public void broadcastMessage(String message) {
        broadcastMessageToAll(message);
    }
}
