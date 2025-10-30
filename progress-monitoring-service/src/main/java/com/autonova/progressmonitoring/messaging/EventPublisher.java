package com.autonova.progressmonitoring.messaging;

public interface EventPublisher {
    void publishToProject(String projectId, String payload);

    void broadcast(String payload);
}
