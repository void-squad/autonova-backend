package com.voidsquad.chatbot.service.workflow;

import com.voidsquad.chatbot.entities.WorkflowStep;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowStepServiceTest {

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private WorkflowStepService workflowStepService;

    private float[] testEmbedding;

    @BeforeEach
    void setUp() {
        testEmbedding = new float[]{0.1f, 0.2f, 0.3f};
    }

    @Test
    void saveWorkflowStep_ShouldGenerateIdWhenNull() {
        // Arrange
        WorkflowStep step = new WorkflowStep();
        step.setName("Test Step");
        step.setDescription("Test Description");
        step.setEmbedding(testEmbedding);

        when(workflowStepRepository.saveWithVector(any(UUID.class), anyString(), anyString(), any(float[].class)))
                .thenAnswer(invocation -> {
                    WorkflowStep savedStep = new WorkflowStep();
                    savedStep.setId(invocation.getArgument(0));
                    savedStep.setName(invocation.getArgument(1));
                    savedStep.setDescription(invocation.getArgument(2));
                    savedStep.setEmbedding(invocation.getArgument(3));
                    return savedStep;
                });

        // Act
        WorkflowStep result = workflowStepService.saveWorkflowStep(step);

        // Assert
        assertNotNull(result.getId());
        verify(workflowStepRepository).saveWithVector(any(UUID.class), eq("Test Step"), eq("Test Description"), eq(testEmbedding));
        verifyNoInteractions(embeddingService); // Should not generate embedding if already present
    }

    @Test
    void saveWorkflowStep_ShouldPreserveExistingId() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        WorkflowStep step = new WorkflowStep();
        step.setId(existingId);
        step.setName("Test Step");
        step.setDescription("Test Description");
        step.setEmbedding(testEmbedding);

        when(workflowStepRepository.saveWithVector(any(UUID.class), anyString(), anyString(), any(float[].class)))
                .thenAnswer(invocation -> {
                    WorkflowStep savedStep = new WorkflowStep();
                    savedStep.setId(invocation.getArgument(0));
                    savedStep.setName(invocation.getArgument(1));
                    savedStep.setDescription(invocation.getArgument(2));
                    savedStep.setEmbedding(invocation.getArgument(3));
                    return savedStep;
                });

        // Act
        WorkflowStep result = workflowStepService.saveWorkflowStep(step);

        // Assert
        assertEquals(existingId, result.getId());
        verify(workflowStepRepository).saveWithVector(eq(existingId), eq("Test Step"), eq("Test Description"), eq(testEmbedding));
    }

    @Test
    void saveWorkflowStep_ShouldGenerateEmbeddingWhenNull() {
        // Arrange
        WorkflowStep step = new WorkflowStep();
        step.setName("Test Step");
        step.setDescription("Test Description");
        step.setEmbedding(null);

        float[] generatedEmbedding = new float[]{0.4f, 0.5f, 0.6f};
        when(embeddingService.generateEmbedding("Test Step: Test Description")).thenReturn(generatedEmbedding);
        when(workflowStepRepository.saveWithVector(any(UUID.class), anyString(), anyString(), any(float[].class)))
                .thenAnswer(invocation -> {
                    WorkflowStep savedStep = new WorkflowStep();
                    savedStep.setId(invocation.getArgument(0));
                    savedStep.setName(invocation.getArgument(1));
                    savedStep.setDescription(invocation.getArgument(2));
                    savedStep.setEmbedding(invocation.getArgument(3));
                    return savedStep;
                });

        // Act
        WorkflowStep result = workflowStepService.saveWorkflowStep(step);

        // Assert
        assertNotNull(result);
        verify(embeddingService).generateEmbedding("Test Step: Test Description");
        verify(workflowStepRepository).saveWithVector(any(UUID.class), eq("Test Step"), eq("Test Description"), eq(generatedEmbedding));
    }

    @Test
    void addWorkflowStep_ShouldCreateNewStepWithGeneratedEmbedding() {
        // Arrange
        String name = "New Step";
        String description = "New Description";
        float[] generatedEmbedding = new float[]{0.7f, 0.8f, 0.9f};

        when(embeddingService.generateEmbedding(name + ": " + description)).thenReturn(generatedEmbedding);
        when(workflowStepRepository.save(any(WorkflowStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WorkflowStep result = workflowStepService.addWorkflowStep(name, description);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(name, result.getName());
        assertEquals(description, result.getDescription());
        assertArrayEquals(generatedEmbedding, result.getEmbedding());
        
        verify(embeddingService).generateEmbedding(name + ": " + description);
        verify(workflowStepRepository).save(any(WorkflowStep.class));
    }

    @Test
    void findSimilarSteps_WithEmbedding_ShouldReturnResults() {
        // Arrange
        float[] queryEmbedding = new float[]{0.1f, 0.2f};
        int limit = 5;
        
        WorkflowStep step1 = new WorkflowStep();
        step1.setId(UUID.randomUUID());
        step1.setName("Step 1");
        
        WorkflowStep step2 = new WorkflowStep();
        step2.setId(UUID.randomUUID());
        step2.setName("Step 2");
        
        List<WorkflowStep> expectedSteps = Arrays.asList(step1, step2);
        when(workflowStepRepository.findSimilarSteps(queryEmbedding, limit)).thenReturn(expectedSteps);

        // Act
        List<WorkflowStep> result = workflowStepService.findSimilarSteps(queryEmbedding, limit);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedSteps, result);
        verify(workflowStepRepository).findSimilarSteps(queryEmbedding, limit);
    }

    @Test
    void findSimilarSteps_WithText_ShouldGenerateEmbeddingAndReturnResults() {
        // Arrange
        String searchText = "find similar steps";
        int limit = 3;
        float[] generatedEmbedding = new float[]{0.3f, 0.4f};
        
        WorkflowStep step = new WorkflowStep();
        step.setId(UUID.randomUUID());
        step.setName("Similar Step");
        
        List<WorkflowStep> expectedSteps = Arrays.asList(step);
        
        when(embeddingService.generateEmbedding(searchText)).thenReturn(generatedEmbedding);
        when(workflowStepRepository.findSimilarSteps(generatedEmbedding, limit)).thenReturn(expectedSteps);

        // Act
        List<WorkflowStep> result = workflowStepService.findSimilarSteps(searchText, limit);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Similar Step", result.get(0).getName());
        
        verify(embeddingService).generateEmbedding(searchText);
        verify(workflowStepRepository).findSimilarSteps(generatedEmbedding, limit);
    }

    @Test
    void findSimilarSteps_WithText_ShouldHandleEmptyResults() {
        // Arrange
        String searchText = "no matches";
        int limit = 5;
        float[] generatedEmbedding = new float[]{0.5f, 0.6f};
        
        when(embeddingService.generateEmbedding(searchText)).thenReturn(generatedEmbedding);
        when(workflowStepRepository.findSimilarSteps(generatedEmbedding, limit)).thenReturn(Arrays.asList());

        // Act
        List<WorkflowStep> result = workflowStepService.findSimilarSteps(searchText, limit);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void saveWorkflowStep_ShouldHandleComplexDescriptions() {
        // Arrange
        WorkflowStep step = new WorkflowStep();
        step.setName("Complex Step");
        step.setDescription("A very long description with special chars: @#$%^&*()");
        step.setEmbedding(null);

        float[] generatedEmbedding = new float[]{1.0f, 2.0f};
        when(embeddingService.generateEmbedding(anyString())).thenReturn(generatedEmbedding);
        when(workflowStepRepository.saveWithVector(any(UUID.class), anyString(), anyString(), any(float[].class)))
                .thenAnswer(invocation -> {
                    WorkflowStep savedStep = new WorkflowStep();
                    savedStep.setId(invocation.getArgument(0));
                    savedStep.setName(invocation.getArgument(1));
                    savedStep.setDescription(invocation.getArgument(2));
                    savedStep.setEmbedding(invocation.getArgument(3));
                    return savedStep;
                });

        // Act
        WorkflowStep result = workflowStepService.saveWorkflowStep(step);

        // Assert
        assertNotNull(result);
        assertEquals("Complex Step", result.getName());
        assertEquals("A very long description with special chars: @#$%^&*()", result.getDescription());
    }
}
