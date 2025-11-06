package com.voidsquad.chatbot.repository;

import com.voidsquad.chatbot.service.workflow.workflowStep.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

//    @Query(value = """
//        SELECT * FROM workflow_steps
//        ORDER BY embedding <-> CAST(:queryEmbedding AS vector)
//        LIMIT 5
//    """, nativeQuery = true)
//    List<WorkflowStep> findNearest(@Param("queryEmbedding") String queryEmbedding);
}