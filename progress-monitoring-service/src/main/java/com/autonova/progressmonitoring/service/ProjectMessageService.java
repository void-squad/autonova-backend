package com.autonova.progressmonitoring.service;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.entity.ProjectMessage;
import com.autonova.progressmonitoring.mapper.ProjectMessageMapper;
import com.autonova.progressmonitoring.repository.ProjectMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    public ProjectMessageDto saveMessage(UUID projectId, String category, String message, String payload, OffsetDateTime occurredAt) {
        ProjectMessage pm = new ProjectMessage(projectId, category, message, payload, occurredAt);
        ProjectMessage saved = repository.save(pm);
        return ProjectMessageMapper.toDto(saved);
    }

    @Transactional
    public ProjectMessageDto saveMessage(ProjectMessageDto dto) {
        ProjectMessage pm = ProjectMessageMapper.toEntity(dto);
        ProjectMessage saved = repository.save(pm);
        return ProjectMessageMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectMessage> getMessagesForProject(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMessageDto> getMessagesForProjectDto(UUID projectId) {
        List<ProjectMessage> list = repository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return ProjectMessageMapper.toDtos(list);
    }

    // Pagination / timeline helpers
    @Transactional(readOnly = true)
    public Slice<ProjectMessageDto> getMessagesPage(UUID projectId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Slice<ProjectMessage> slice = repository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        return slice.map(ProjectMessageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectMessageDto> getMessagesBefore(UUID projectId, OffsetDateTime before, int size) {
        Pageable pageable = PageRequest.of(0, Math.max(1, size));
        Slice<ProjectMessage> slice = repository.findByProjectIdAndCreatedAtBeforeOrderByCreatedAtDesc(projectId, before, pageable);
        return slice.map(ProjectMessageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectMessageDto> getMessagesAfter(UUID projectId, OffsetDateTime after, int size) {
        Pageable pageable = PageRequest.of(0, Math.max(1, size));
        Slice<ProjectMessage> slice = repository.findByProjectIdAndCreatedAtAfterOrderByCreatedAtAsc(projectId, after, pageable);
        return slice.map(ProjectMessageMapper::toDto);
    }
}
