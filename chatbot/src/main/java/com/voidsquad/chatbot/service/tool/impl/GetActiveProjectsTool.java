package com.voidsquad.chatbot.service.tool.impl;

import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Tool that fetches active projects from the project-service HTTP endpoint.
 *
 * It performs a simple GET to http://localhost:8083/api/projects/active and
 * returns the response body as the tool result.
 */
@Component
public class GetActiveProjectsTool implements Tool {

    private static final String DEFAULT_URL = "http://localhost:8083/api/projects/active";
    private final RestTemplate restTemplate;

    public GetActiveProjectsTool() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String name() {
        return "getActiveProjects";
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String url = DEFAULT_URL;
        // allow callers to override the URL via parameters if necessary
        if (request != null && request.getParameters() != null) {
            Object u = request.getParameters().get("url");
            if (u instanceof String && !((String) u).isBlank()) url = (String) u;
        }

        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                String body = resp.getBody() == null ? "" : resp.getBody();
                return ToolCallResult.success(body, name());
            } else {
                return ToolCallResult.failure("Remote service returned status: " + resp.getStatusCode(), name());
            }
        } catch (Exception e) {
            return ToolCallResult.failure("Failed to call project service: " + e.getMessage(), name());
        }
    }
}
