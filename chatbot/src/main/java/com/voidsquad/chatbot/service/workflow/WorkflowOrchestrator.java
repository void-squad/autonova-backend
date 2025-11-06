package com.voidsquad.chatbot.service.workflow;

import com.voidsquad.chatbot.service.languageProcessor.LanguageProcessor;
import com.voidsquad.chatbot.service.msgqueue.RabbitTemplate;
import com.voidsquad.chatbot.service.workflow.workflowStep.StepSequencer;
import com.voidsquad.chatbot.service.workflow.workflowStep.StepValidator;
import com.voidsquad.chatbot.service.workflow.workflowStep.WorkflowStep;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowOrchestrator {

//    private final WorkflowRetriever workflowRetriever;
//    private final LanguageProcessor languageProcessor;
//    private final StepValidator stepValidator;
//    private final StepSequencer stepSequencer;
//    private final RabbitTemplate rabbitTemplate;

//    @Autowired
//    public WorkflowOrchestrator(
//            WorkflowRetriever workflowRetriever,
//            LanguageProcessor languageProcessor,
//            StepValidator stepValidator,
//            StepSequencer stepSequencer,
//            RabbitTemplate rabbitTemplate) {
//        this.workflowRetriever = workflowRetriever;
//        this.languageProcessor = languageProcessor;
//        this.stepValidator = stepValidator;
//        this.stepSequencer = stepSequencer;
//        this.rabbitTemplate = rabbitTemplate;
//    }

    @RabbitListener(queues = "workflow.requests")
    public void handleWorkflowRequest(WorkflowRequestEvent event) {
//        try {
//            System.out.println("🔹 Received workflow request: " + event.getPrompt());
//
//            // Step 1: Retrieve candidate workflows (Vector + DB)
//            List<WorkflowStep> candidates = workflowRetriever.findCandidates(event.getPrompt(), event.getUserRole());
//
//            // Step 2: Use LLM to refine / extend steps
//            List<WorkflowStep> refinedSteps = languageProcessor.refineSteps(event.getPrompt(), candidates);
//
//            // Step 3: Validate steps (duplicates, permissions, required data)
//            List<WorkflowStep> validSteps = stepValidator.validate(refinedSteps, event.getUserData());
//
//            // Step 4: Sequence steps
//            List<WorkflowStep> orderedSteps = stepSequencer.order(validSteps);
//
//            // Step 5: Produce final result (LLM summarization / formatting)
//            String result = languageProcessor.generateFinalOutput(event.getPrompt(), orderedSteps);
//
//            // Step 6: Publish response
//            WorkflowResultEvent resultEvent = new WorkflowResultEvent(event.getUserId(), result);
//            rabbitTemplate.convertAndSend("workflow.results", resultEvent);
//
//        } catch (Exception e) {
//            System.err.println("WorkflowOrchestrator Error: " + e.getMessage());
//            // optionally send failure event
//        }
    }
}
