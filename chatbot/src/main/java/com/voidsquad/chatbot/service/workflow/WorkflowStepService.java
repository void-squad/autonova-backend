package com.voidsquad.chatbot.service.workflow;

import com.voidsquad.chatbot.entities.WorkflowStep;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkflowStepService {

    private final WorkflowStepRepository workflowStepRepository;
    private final EmbeddingService embeddingService; // Changed to Ollama service

    public WorkflowStepService(WorkflowStepRepository workflowStepRepository,
                               EmbeddingService embeddingService) {
        this.workflowStepRepository = workflowStepRepository;
        this.embeddingService = embeddingService;

    }

    public WorkflowStep saveWorkflowStep(WorkflowStep step) {
        if (step.getId() == null) {
            step.setId(UUID.randomUUID());
        }

        if (step.getEmbedding() == null) {
            String textToEmbed = step.getName() + ": " + step.getDescription();
            float[] embedding = embeddingService.generateEmbedding(textToEmbed);
            step.setEmbedding(embedding);
        }

        return workflowStepRepository.saveWithVector(
                step.getId(),
                step.getName(),
                step.getDescription(),
                step.getEmbedding()
        );
    }

    public WorkflowStep addWorkflowStep(String name, String description) {
        String textToEmbed = name + ": " + description;
        float[] embedding = embeddingService.generateEmbedding(textToEmbed);

        WorkflowStep step = new WorkflowStep();
        step.setId(UUID.randomUUID());
        step.setName(name);
        step.setDescription(description);
        step.setEmbedding(embedding); // 384-dimensional vector from all-minilm

        return workflowStepRepository.save(step);
    }

    public List<WorkflowStep> findSimilarSteps(float[] embedding, int limit) {
        return workflowStepRepository.findSimilarSteps(embedding, limit);
    }

    public List<WorkflowStep> findSimilarSteps(String text, int limit) {
        float[] embedding = embeddingService.generateEmbedding(text);
        return findSimilarSteps(embedding, limit);
    }
}