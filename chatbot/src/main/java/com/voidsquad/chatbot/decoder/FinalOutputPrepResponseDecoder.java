package com.voidsquad.chatbot.decoder;

import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.util.DecodeResult;
import com.voidsquad.chatbot.util.JsonPathKeyDecoder;
import com.voidsquad.chatbot.util.TypeDescriptor;
import com.voidsquad.chatbot.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Decode the final-output LLM response which is expected to follow the FinalOutputStrategy schema:
 * {"isComplete": true|false, "data": "msg to user"}
 */
@Component
public class FinalOutputPrepResponseDecoder {
    private static final Logger log = LoggerFactory.getLogger(FinalOutputPrepResponseDecoder.class);
    private final JsonPathKeyDecoder decoder;

    public FinalOutputPrepResponseDecoder(JsonPathKeyDecoder decoder) {
        this.decoder = decoder != null ? decoder : new JsonPathKeyDecoder();
    }

    public FinalOutputPrepResponseDecoder() {
        this(null);
    }

    /**
     * Decode the processing result and return the user-facing string from the `data` field.
     * If the LLM indicates isComplete=false we still return the data but log a warning.
     */
    public String decode(ProcessingResult result) {
        if (result == null) return "";

        String output = result.output();
        if (output == null || output.isBlank()) return "";

        // strip common fences and URL-encoding
        String cleaned = TextUtil.stripCodeFences(output).trim();
        for (int i = 0; i < 3 && cleaned.contains("%"); i++) {
            try {
                cleaned = URLDecoder.decode(cleaned, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.debug("URL decode failed: {}", e.getMessage());
                break;
            }
        }

        // strip leading non-json
        int idx = cleaned.indexOf('{');
        if (idx > 0) cleaned = cleaned.substring(idx);

        try {
            Map<String, TypeDescriptor> schema = new HashMap<>();
            schema.put("$.isComplete", TypeDescriptor.of(TypeDescriptor.Primitive.BOOLEAN));
            schema.put("$.data", TypeDescriptor.of(TypeDescriptor.Primitive.STRING));

            DecodeResult dr = decoder.decode(cleaned, schema);

            Object isCompleteObj = dr.getValues().get("$.isComplete");
            boolean isComplete = false;
            if (isCompleteObj instanceof Boolean b) isComplete = b;
            else if (isCompleteObj instanceof String s) isComplete = Boolean.parseBoolean(s);

            Object dataObj = dr.getValues().get("$.data");
            String data = dataObj == null ? "" : String.valueOf(dataObj);

            if (!isComplete) log.warn("FinalOutputPrep: LLM marked response as incomplete");
            if (dr.hasErrors()) log.debug("FinalOutputPrep decode errors: {}", dr.getErrors());

            return data;
        } catch (Exception e) {
            log.warn("Failed to decode final output: {}", e.getMessage());
            return output;
        }
    }
}
