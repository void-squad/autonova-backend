package com.voidsquad.chatbot.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

//@Entity
//@Table(name = "workflow_steps")
public class WorkflowStep {

//    @Id
//    @GeneratedValue(generator = "UUID")
//    @GenericGenerator(
//            name = "UUID",
//            strategy = "org.hibernate.id.UUIDGenerator"
//    )
//    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
//    private UUID id;
//
//    @Column(name = "step_name", nullable = false)
//    private String stepName;
//
//    @Column(name = "step_description")
//    private String stepDescription;
//
//    @ElementCollection
//    @CollectionTable(name = "workflow_allowed_users", joinColumns = @JoinColumn(name = "workflow_id"))
//    @Column(name = "user_role")
//    private List<String> allowedUsers;
//
//    @ElementCollection
//    @CollectionTable(name = "workflow_required_data", joinColumns = @JoinColumn(name = "workflow_id"))
//    @Column(name = "data_key")
//    private List<String> requiredLocalData;
//
//    @ElementCollection
//    @CollectionTable(name = "workflow_related_steps", joinColumns = @JoinColumn(name = "workflow_id"))
//    @Column(name = "related_step")
//    private List<String> relatedSteps;
//
//    @Column(name = "prompt_template", columnDefinition = "TEXT")
//    private String promptTemplate;
//
//    // Store vector embeddings as float[]
//    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    private float[] embedding;
//
//    // Constructors
//    public WorkflowStep() {}
//
//    public WorkflowStep(String stepName, String stepDescription, List<String> allowedUsers,
//                        List<String> requiredLocalData, List<String> relatedSteps,
//                        String promptTemplate, float[] embedding) {
//        this.stepName = stepName;
//        this.stepDescription = stepDescription;
//        this.allowedUsers = allowedUsers;
//        this.requiredLocalData = requiredLocalData;
//        this.relatedSteps = relatedSteps;
//        this.promptTemplate = promptTemplate;
//        this.embedding = embedding;
//    }
//
//    // Getters and Setters
//    public UUID getId() {
//        return id;
//    }
//
//    public void setId(UUID id) {
//        this.id = id;
//    }
//
//    public String getStepName() {
//        return stepName;
//    }
//
//    public void setStepName(String stepName) {
//        this.stepName = stepName;
//    }
//
//    public String getStepDescription() {
//        return stepDescription;
//    }
//
//    public void setStepDescription(String stepDescription) {
//        this.stepDescription = stepDescription;
//    }
//
//    public List<String> getAllowedUsers() {
//        return allowedUsers;
//    }
//
//    public void setAllowedUsers(List<String> allowedUsers) {
//        this.allowedUsers = allowedUsers;
//    }
//
//    public List<String> getRequiredLocalData() {
//        return requiredLocalData;
//    }
//
//    public void setRequiredLocalData(List<String> requiredLocalData) {
//        this.requiredLocalData = requiredLocalData;
//    }
//
//    public List<String> getRelatedSteps() {
//        return relatedSteps;
//    }
//
//    public void setRelatedSteps(List<String> relatedSteps) {
//        this.relatedSteps = relatedSteps;
//    }
//
//    public String getPromptTemplate() {
//        return promptTemplate;
//    }
//
//    public void setPromptTemplate(String promptTemplate) {
//        this.promptTemplate = promptTemplate;
//    }
//
//    public float[] getEmbedding() {
//        return embedding;
//    }
//
//    public void setEmbedding(float[] embedding) {
//        this.embedding = embedding;
//    }
}
