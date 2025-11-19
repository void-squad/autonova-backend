package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.WorkflowStepDTO;
import com.voidsquad.chatbot.entities.WorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStepMapperTest {

    private WorkflowStepMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkflowStepMapper();
    }

    @Test
    void toDTO_ShouldMapAllFieldsCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        
        WorkflowStep entity = new WorkflowStep();
        entity.setId(id);
        entity.setName("Test Step");
        entity.setDescription("Test Description");
        entity.setEmbedding(embedding);

        // Act
        WorkflowStepDTO dto = mapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("Test Step", dto.getName());
        assertEquals("Test Description", dto.getDescription());
        assertNotNull(dto.getEmbedding());
        assertArrayEquals(embedding, dto.getEmbedding());
        assertNotSame(embedding, dto.getEmbedding(), "Embedding should be cloned");
    }

    @Test
    void toDTO_ShouldHandleNullInput() {
        // Act
        WorkflowStepDTO dto = mapper.toDTO(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void toDTO_ShouldHandleNullEmbedding() {
        // Arrange
        WorkflowStep entity = new WorkflowStep();
        entity.setId(UUID.randomUUID());
        entity.setName("Test");
        entity.setDescription("Description");
        entity.setEmbedding(null);

        // Act
        WorkflowStepDTO dto = mapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getEmbedding());
    }

    @Test
    void toEntity_ShouldMapAllFieldsCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.4f, 0.5f, 0.6f};
        
        WorkflowStepDTO dto = WorkflowStepDTO.builder()
                .id(id)
                .name("DTO Step")
                .description("DTO Description")
                .embedding(embedding)
                .build();

        // Act
        WorkflowStep entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("DTO Step", entity.getName());
        assertEquals("DTO Description", entity.getDescription());
        assertNotNull(entity.getEmbedding());
        assertArrayEquals(embedding, entity.getEmbedding());
        assertNotSame(embedding, entity.getEmbedding(), "Embedding should be cloned");
    }

    @Test
    void toEntity_ShouldHandleNullInput() {
        // Act
        WorkflowStep entity = mapper.toEntity(null);

        // Assert
        assertNull(entity);
    }

    @Test
    void toEntity_ShouldHandleNullEmbedding() {
        // Arrange
        WorkflowStepDTO dto = WorkflowStepDTO.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .description("Desc")
                .embedding(null)
                .build();

        // Act
        WorkflowStep entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertNull(entity.getEmbedding());
    }

    @Test
    void updateEntityFromDTO_ShouldUpdateAllFields() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        float[] newEmbedding = new float[]{0.7f, 0.8f, 0.9f};
        
        WorkflowStep entity = new WorkflowStep();
        entity.setId(existingId);
        entity.setName("Old Name");
        entity.setDescription("Old Description");
        entity.setEmbedding(new float[]{0.1f, 0.2f});

        WorkflowStepDTO dto = WorkflowStepDTO.builder()
                .id(UUID.randomUUID()) // Different ID - should not update ID
                .name("New Name")
                .description("New Description")
                .embedding(newEmbedding)
                .build();

        // Act
        mapper.updateEntityFromDTO(dto, entity);

        // Assert
        assertEquals(existingId, entity.getId(), "ID should not be updated");
        assertEquals("New Name", entity.getName());
        assertEquals("New Description", entity.getDescription());
        assertArrayEquals(newEmbedding, entity.getEmbedding());
        assertNotSame(newEmbedding, entity.getEmbedding(), "Embedding should be cloned");
    }

    @Test
    void updateEntityFromDTO_ShouldHandleNullDTO() {
        // Arrange
        WorkflowStep entity = new WorkflowStep();
        entity.setId(UUID.randomUUID());
        entity.setName("Original");
        entity.setDescription("Original Desc");

        String originalName = entity.getName();
        String originalDesc = entity.getDescription();

        // Act
        mapper.updateEntityFromDTO(null, entity);

        // Assert - Entity should remain unchanged
        assertEquals(originalName, entity.getName());
        assertEquals(originalDesc, entity.getDescription());
    }

    @Test
    void updateEntityFromDTO_ShouldHandleNullEntity() {
        // Arrange
        WorkflowStepDTO dto = WorkflowStepDTO.builder()
                .name("Test")
                .description("Test Desc")
                .build();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> mapper.updateEntityFromDTO(dto, null));
    }

    @Test
    void updateEntityFromDTO_ShouldHandleBothNull() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> mapper.updateEntityFromDTO(null, null));
    }

    @Test
    void updateEntityFromDTO_ShouldSetNullEmbedding() {
        // Arrange
        WorkflowStep entity = new WorkflowStep();
        entity.setEmbedding(new float[]{0.1f, 0.2f});

        WorkflowStepDTO dto = WorkflowStepDTO.builder()
                .name("Test")
                .description("Test")
                .embedding(null)
                .build();

        // Act
        mapper.updateEntityFromDTO(dto, entity);

        // Assert
        assertNull(entity.getEmbedding());
    }
}
