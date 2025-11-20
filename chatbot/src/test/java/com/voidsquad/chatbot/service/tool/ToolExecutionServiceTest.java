package com.voidsquad.chatbot.service.tool;

import com.voidsquad.chatbot.model.ToolCall;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolExecutionServiceTest {

    static class DummyTool implements Tool {
        private final String name;
        DummyTool(String name) { this.name = name; }
        @Override public String name() { return name; }

        @Override
        public ToolCallResult execute(ToolCallRequest request) {
            String params = String.valueOf(request.getParameters());
            String userInput = params.contains("userInput") ? String.valueOf(request.getParameters().get("userInput")) : "null";
            String user = "null";
            if (params.contains("userInfo") && request.getParameters().get("userInfo") instanceof AuthInfo) {
                AuthInfo ai = (AuthInfo) request.getParameters().get("userInfo");
                user = ai.getUserId() != null ? String.valueOf(ai.getUserId()) : "null";
            }
            return ToolCallResult.success("ok:params=" + params + " userInput=" + userInput + " userId=" + user, name());
        }
    }

    @Test
    void executesRegisteredToolAndReturnsResult() {
        DummyTool t = new DummyTool("d1");
        ToolRegistry reg = new ToolRegistry(List.of(t));
        ToolExecutionService svc = new ToolExecutionService(reg);

        AuthInfo auth = AuthInfo.builder().userId(42L).firstName("Test").email("t@t.com").build();
        Map<String, Object> params = new HashMap<>();
        params.put("q", "x");
        ToolCall call = new ToolCall("d1", params, "explain");
        List<ToolCallResult> res = svc.executeAll(List.of(call), "explain", auth);

        assertEquals(1, res.size());
        assertTrue(res.get(0).isSuccess());
        assertNotNull(res.get(0).getResult());
        assertTrue(res.get(0).getResult().toString().contains("params"));
    }

    @Test
    void unknownToolReturnsFailureResult() {
        ToolRegistry reg = new ToolRegistry(List.of());
        ToolExecutionService svc = new ToolExecutionService(reg);

        ToolCall call = new ToolCall("missing", new HashMap<>(), "x");
        List<ToolCallResult> res = svc.executeAll(List.of(call), "x", null);
        assertEquals(1, res.size());
        assertFalse(res.get(0).isSuccess());
    }
}
