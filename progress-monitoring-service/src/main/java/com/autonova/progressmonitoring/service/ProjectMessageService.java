package com.autonova.progressmonitoring.service;

import com.autonova.progressmonitoring.entity.ProjectMessage;
import com.autonova.progressmonitoring.repository.ProjectMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectMessageService {

    private final ProjectMessageRepository repository;

    public ProjectMessageService(ProjectMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProjectMessage saveMessage(UUID projectId, String category, String message, String payload, OffsetDateTime occurredAt) {
        ProjectMessage pm = new ProjectMessage(projectId, category, message, payload, occurredAt);
        return repository.save(pm);
    }

    @Transactional(readOnly = true)
    public List<ProjectMessage> getMessagesForProject(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
