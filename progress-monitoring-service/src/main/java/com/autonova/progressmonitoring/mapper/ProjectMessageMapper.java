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
        return pm;
    }

    public static List<ProjectMessageDto> toDtos(List<ProjectMessage> list) {
        return list.stream().map(ProjectMessageMapper::toDto).collect(Collectors.toList());
    }
}
