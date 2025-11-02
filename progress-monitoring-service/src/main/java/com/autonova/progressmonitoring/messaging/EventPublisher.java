package com.autonova.progressmonitoring.messaging;

public interface EventPublisher {
    // publish an event to subscribers of a specific project/topic
    void publishToProject(String projectId, String payload);

    void broadcast(String payload);
}
