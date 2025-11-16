package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.WorkflowStepDTO;
import com.voidsquad.chatbot.entities.WorkflowStep;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStepMapper {

    public WorkflowStepDTO toDTO(WorkflowStep entity) {
        if (entity == null) {
            return null;
        }

        return WorkflowStepDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .embedding(entity.getEmbedding() != null ? entity.getEmbedding().clone() : null)
                .description(entity.getDescription())
                .build();
    }

    public WorkflowStep toEntity(WorkflowStepDTO dto) {
        if (dto == null) {
            return null;
        }

        WorkflowStep entity = new WorkflowStep();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setEmbedding(dto.getEmbedding() != null ? dto.getEmbedding().clone() : null);
        entity.setDescription(dto.getDescription());

        return entity;
    }

    public void updateEntityFromDTO(WorkflowStepDTO dto, WorkflowStep entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setEmbedding(dto.getEmbedding() != null ? dto.getEmbedding().clone() : null);
        entity.setDescription(dto.getDescription());
    }
}