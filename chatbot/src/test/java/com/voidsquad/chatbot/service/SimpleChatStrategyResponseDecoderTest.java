package com.voidsquad.chatbot.service;

import com.voidsquad.chatbot.decoder.SimpleAiResponseDecoder;
import com.voidsquad.chatbot.model.SimpleChatStrategyResponse;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class SimpleChatStrategyResponseDecoderTest {

    @Test
    public void decode_happyPath_booleanAndData() {
        String json = "{\"isSimple\": true, \"data\": \"hello-world\"}";
        ProcessingResult pr = new ProcessingResult(json, OutputFormat.JSON, ProcessingType.SIMPLE, Collections.emptyMap());

        SimpleAiResponseDecoder decoder = new SimpleAiResponseDecoder(new com.voidsquad.chatbot.util.JsonPathKeyDecoder());
        SimpleChatStrategyResponse res = decoder.decode(pr);

        Assertions.assertTrue(res.isSimple());
        Assertions.assertEquals("hello-world", res.data());
    }

    @Test
    public void decode_fallbackToMetadata_whenOutputMissing() {
        ProcessingResult pr = new ProcessingResult("", OutputFormat.JSON, ProcessingType.SIMPLE, Collections.singletonMap("data", "meta-value"));
        SimpleAiResponseDecoder decoder = new SimpleAiResponseDecoder(new com.voidsquad.chatbot.util.JsonPathKeyDecoder());
        SimpleChatStrategyResponse res = decoder.decode(pr);

        Assertions.assertFalse(res.isSimple());
        Assertions.assertEquals("meta-value", res.data());
    }
}
