package com.voidsquad.chatbot.decoder;

import com.voidsquad.chatbot.model.SimpleChatStrategyResponse;
import com.voidsquad.chatbot.util.DecodeResult;
import com.voidsquad.chatbot.util.JsonPathKeyDecoder;
import com.voidsquad.chatbot.util.TypeDescriptor;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodes LLM output (ProcessingResult) into {@link SimpleChatStrategyResponse}.
 */
@Component
public class SimpleAiResponseDecoder {
    private static final Logger log = LoggerFactory.getLogger(SimpleAiResponseDecoder.class);
    private final JsonPathKeyDecoder decoder;

    public SimpleAiResponseDecoder(JsonPathKeyDecoder decoder) {
        this.decoder = decoder != null ? decoder : new JsonPathKeyDecoder();
    }

    public SimpleChatStrategyResponse decode(ProcessingResult simpleResult) {
        String strData = "";
        boolean isSimple = false;

        if (simpleResult == null) {
            log.warn("ProcessingResult is null");
            return new SimpleChatStrategyResponse(false, "");
        }

        Object metaDataObj = simpleResult.metadata().get("data");
        if (metaDataObj != null) {
            strData = metaDataObj.toString();
        }

        String output = simpleResult.output();
        if (output == null || output.isEmpty()) {
            log.warn("No output present in ProcessingResult");
            return new SimpleChatStrategyResponse(false, strData);
        }

        // decode up to 3 times if URL-encoded
        String decoded = output;
        for (int i = 0; i < 3 && decoded.contains("%"); i++) {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        }
        decoded = decoded.trim();

        // strip any leading non-JSON text
        int firstBrace = decoded.indexOf('{');
        if (firstBrace > 0) decoded = decoded.substring(firstBrace);

        try {
            Map<String, TypeDescriptor> schema = new HashMap<>();
            schema.put("$.isSimple", TypeDescriptor.of(TypeDescriptor.Primitive.BOOLEAN));
            schema.put("$.data", TypeDescriptor.of(TypeDescriptor.Primitive.STRING));

            DecodeResult res = decoder.decode(decoded, schema);

            Object simpleVal = res.getValues().get("$.isSimple");
            if (simpleVal instanceof Boolean b) isSimple = b;
            else if (simpleVal instanceof String s) isSimple = Boolean.parseBoolean(s);

            Object dataVal = res.getValues().get("$.data");
            if (dataVal != null) strData = dataVal.toString();

            if (res.hasErrors()) {
                log.debug("SimpleAiResponseDecoder decode errors: {}", res.getErrors());
            }

            log.debug("Decoded LLM simple response -> isSimple: {}, data-length: {}", isSimple, strData == null ? 0 : strData.length());
            return new SimpleChatStrategyResponse(isSimple, strData);

        } catch (Exception e) {
            log.warn("Failed to decode simple LLM response: {}", e.getMessage());
            return new SimpleChatStrategyResponse(false, strData);
        }
    }
}
