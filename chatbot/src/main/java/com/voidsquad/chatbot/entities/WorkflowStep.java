package com.voidsquad.chatbot.entities;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.util.UUID;

@Entity
@Table(name = "workflows")
public class WorkflowStep {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Type(DoubleArrayType.class)
    @Column(columnDefinition = "vector(1536)")
    private double[] embedding;

    @Column(columnDefinition = "TEXT")
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

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
