package com.voidsquad.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepDTO {
    private UUID id;
    private String name;
    private float[] embedding;
    private String description;
}