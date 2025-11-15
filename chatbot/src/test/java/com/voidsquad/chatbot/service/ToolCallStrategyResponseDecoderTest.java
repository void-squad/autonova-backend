package com.voidsquad.chatbot.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ToolCallStrategyResponseDecoderTest {
    private ToolCallResponseDecoder decoder;

    @BeforeEach
    void setUp() {
        this.decoder = new ToolCallResponseDecoder(new ObjectMapper());
    }

    @Test
    void happyPath_singleToolCallIsParsed() {
        String json = "{\n" +
                "  \"tool_calls\": [\n" +
                "    {\n" +
                "      \"tool_name\": \"search\",\n" +
                "      \"parameters\": { \"q\": \"term\" },\n" +
                "      \"explanation\": \"Find stuff.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        List<ToolCall> out = decoder.decode(json);
        assertNotNull(out);
        assertEquals(1, out.size());
        ToolCall t = out.get(0);
        assertEquals("search", t.getToolName());
        assertNotNull(t.getParameters());
        assertEquals("term", t.getParameters().get("q"));
        assertEquals("Find stuff.", t.getExplanation());
    }

    @Test
    void malformedInput_returnsEmptyList() {
        List<ToolCall> out = decoder.decode("no json here");
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    void textWithPrefix_findJsonAndParse() {
        String prefixed = "Some commentary before the JSON: {\"tool_calls\":[{\"tool_name\":\"t\",\"parameters\":{},\"explanation\":\"x\"}]}";
        List<ToolCall> out = decoder.decode(prefixed);
        assertEquals(1, out.size());
        assertEquals("t", out.get(0).getToolName());
    }
}
