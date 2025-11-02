package com.autonova.progressmonitoring.messaging.publisher;

public interface EventPublisher {
    // publish an event to subscribers of a specific project/topic
    void publishToProject(String projectId, String payload);

    void broadcast(String payload);

    // publish a human-friendly message derived from an event to subscribers of a specific project/topic
    void publishMessageToProject(String projectId, String message);

    // broadcast a human-friendly message to all subscribers
    void broadcastMessage(String message);
}
