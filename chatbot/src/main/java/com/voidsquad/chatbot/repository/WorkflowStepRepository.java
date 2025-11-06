package com.voidsquad.chatbot.repository;

import com.voidsquad.chatbot.entities.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

//    @Query(value = """
//        SELECT * FROM workflow_steps
//        ORDER BY embedding <-> CAST(:queryEmbedding AS vector)
//        LIMIT 5
//    """, nativeQuery = true)
//    List<WorkflowStep> findNearest(@Param("queryEmbedding") String queryEmbedding);

    @Query(value = "INSERT INTO workflow_steps (id, step_name, step_description, embedding) VALUES (:id, :name, :description, CAST(:embedding AS vector)) RETURNING *",
            nativeQuery = true)
    WorkflowStep saveWithVector(@Param("id") UUID id,
                                @Param("name") String name,
                                @Param("description") String description,
                                @Param("embedding") float[] embedding);

    @Query(value = "INSERT INTO workflow_steps (id, step_name, step_description, embedding) VALUES (:#{#step.id}, :#{#step.name}, :#{#step.description}, CAST(:#{#step.embedding} AS vector)) RETURNING *",
            nativeQuery = true)
    WorkflowStep saveWithVector(@Param("step") WorkflowStep step);

    @Query(value = "SELECT * FROM workflow_steps ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit",
            nativeQuery = true)
    List<WorkflowStep> findSimilarSteps(@Param("embedding") float[] embedding,
                                        @Param("limit") int limit);
}