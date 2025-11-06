package com.voidsquad.chatbot.entities;

import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import com.voidsquad.chatbot.converter.FloatArrayConverter;
import io.hypersistence.utils.hibernate.type.array.FloatArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    private UUID id;

    @Column(nullable = false, name="step_name")
    private String name;

    @Column(name = "embedding", columnDefinition = "vector(384)")
    @Convert(converter = FloatArrayConverter.class)
    private float[] embedding;

    @Column(name="step_description",columnDefinition = "TEXT")
    private String description;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


}
