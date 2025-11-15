package com.voidsquad.chatbot.service.tool;

import com.voidsquad.chatbot.model.ToolCall;
import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolExecutionServiceTest {

    static class DummyTool implements Tool {
        private final String name;
        DummyTool(String name) { this.name = name; }
        @Override public String name() { return name; }
        @Override public ToolCallResult execute(ToolCallRequest request) { return ToolCallResult.success("ok:" + request.getParameters(),name()); }
    }

    @Test
    void executesRegisteredToolAndReturnsResult() {
        DummyTool t = new DummyTool("d1");
        ToolRegistry reg = new ToolRegistry(List.of(t));
        ToolExecutionService svc = new ToolExecutionService(reg);

        ToolCall call = new ToolCall("d1", Map.of("q", "x"), "explain");
        List<ToolCallResult> res = svc.executeAll(List.of(call));

        assertEquals(1, res.size());
        assertTrue(res.get(0).isSuccess());
        assertNotNull(res.get(0).getResult());
    }

    @Test
    void unknownToolReturnsFailureResult() {
        ToolRegistry reg = new ToolRegistry(List.of());
        ToolExecutionService svc = new ToolExecutionService(reg);

        ToolCall call = new ToolCall("missing", Map.of(), "x");
        List<ToolCallResult> res = svc.executeAll(List.of(call));
        assertEquals(1, res.size());
        assertFalse(res.get(0).isSuccess());
    }
}
