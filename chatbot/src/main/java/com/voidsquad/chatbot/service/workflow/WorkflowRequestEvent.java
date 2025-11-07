package com.voidsquad.chatbot.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRequestEvent {
    private String userId;
    private String userRole;
    private String prompt;
    private Map<String, Object> userData;
}
