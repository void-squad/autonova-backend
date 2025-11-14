package com.autonova.progressmonitoring.mapper;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.entity.ProjectMessage;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectMessageMapper {

    private ProjectMessageMapper() { }

    public static ProjectMessageDto toDto(ProjectMessage pm) {
        if (pm == null) return null;
        return ProjectMessageDto.builder()
                .id(pm.getId())
                .projectId(pm.getProjectId())
                .category(pm.getCategory())
                .message(pm.getMessage())
                .payload(pm.getPayload())
                .occurredAt(pm.getOccurredAt())
                .createdAt(pm.getCreatedAt())
                .attachmentUrl(pm.getAttachmentUrl())
                .attachmentContentType(pm.getAttachmentContentType())
                .attachmentFilename(pm.getAttachmentFilename())
                .attachmentSize(pm.getAttachmentSize())
                .build();
    }

    public static ProjectMessage toEntity(ProjectMessageDto dto) {
        if (dto == null) return null;
        ProjectMessage pm = new ProjectMessage(
                dto.getProjectId(),
                dto.getCategory(),
                dto.getMessage(),
                dto.getPayload(),
                dto.getOccurredAt()
        );
        pm.setAttachmentUrl(dto.getAttachmentUrl());
        pm.setAttachmentContentType(dto.getAttachmentContentType());
        pm.setAttachmentFilename(dto.getAttachmentFilename());
        pm.setAttachmentSize(dto.getAttachmentSize());
        return pm;
    }

    public static List<ProjectMessageDto> toDtos(List<ProjectMessage> list) {
        return list.stream().map(ProjectMessageMapper::toDto).collect(Collectors.toList());
    }
}
