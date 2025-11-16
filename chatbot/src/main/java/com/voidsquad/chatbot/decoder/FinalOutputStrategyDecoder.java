package com.voidsquad.chatbot.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Produces a final output JSON string following the FinalOutputStrategy contract:
 * {"isComplete": true|false, "data": "..."}
 */
@Component
public class FinalOutputStrategyDecoder {
    private static final Logger log = LoggerFactory.getLogger(FinalOutputStrategyDecoder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FinalOutputStrategyResponse decode(ProcessingResult finalResult) {
        String raw = finalResult == null || finalResult.output() == null ? "" : finalResult.output().toString();
        String cleaned = TextUtil.stripCodeFences(raw).trim();

        // try URL decode up to a few times
        for (int i = 0; i < 3 && cleaned.contains("%"); i++) {
            try {
                cleaned = java.net.URLDecoder.decode(cleaned, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.debug("URL decode failed: {}", e.getMessage());
                break;
            }
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            boolean hasIsComplete = root.has("isComplete");
            boolean hasData = root.has("data");
            if (hasIsComplete && hasData) {
                boolean isComplete = root.path("isComplete").asBoolean(false);
                String data = root.path("data").asText("");
                return new FinalOutputStrategyResponse(isComplete, data);
            } else {
                return new FinalOutputStrategyResponse(false, cleaned);
            }
        } catch (Exception e) {
            log.debug("final output not valid JSON: {}", e.getMessage());
            return new FinalOutputStrategyResponse(false, cleaned);
        }
    }
}
