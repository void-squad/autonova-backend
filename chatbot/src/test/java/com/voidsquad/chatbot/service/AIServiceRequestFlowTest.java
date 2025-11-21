package com.voidsquad.chatbot.service;

import com.voidsquad.chatbot.entities.StaticInfo;
import com.voidsquad.chatbot.exception.JsonDecodeException;
import com.voidsquad.chatbot.exception.NoAnswerException;
import com.voidsquad.chatbot.model.SimpleChatStrategyResponse;
import com.voidsquad.chatbot.model.ToolCall;
import com.voidsquad.chatbot.repository.StaticInfoRepository;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.language.LanguageProcessor;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import com.voidsquad.chatbot.service.tool.ToolExecutionService;
import com.voidsquad.chatbot.service.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceRequestFlowTest {

    @Mock LanguageProcessor languageProcessor;
    @Mock StaticInfoRepository staticInfoRepository;
    @Mock WorkflowStepRepository workflowStepRepository;
    @Mock ToolRegistry toolRegistry;
    @Mock ToolExecutionService toolExecutionService;
    @Mock com.voidsquad.chatbot.decoder.SimpleAiResponseDecoder simpleDecoder;
    @Mock com.voidsquad.chatbot.decoder.FinalOutputStrategyDecoder finalDecoder;
    @Mock com.voidsquad.chatbot.decoder.ToolCallResponseDecoder toolCallDecoder;
    @Mock com.voidsquad.chatbot.service.embedding.EmbeddingService embeddingService;

    @InjectMocks AIService aiService;

    @Test
    void requestHandler_simplePath_returnsSimpleData() throws JsonDecodeException, NoAnswerException, IOException {
        // Embedding + static hits
        when(embeddingService.generateEmbedding(any())).thenReturn(new float[]{1f,2f,3f});
        when(staticInfoRepository.findSimilarStaticInfo(any(), anyInt())).thenReturn(List.of(
                StaticInfo.builder().topic("T").description("D").embedding(new float[]{1f}).build()
        ));
        // First LLM call
        ProcessingResult simpleResult = new ProcessingResult("{\"isSimple\":true,\"data\":\"Hi\"}", OutputFormat.JSON, ProcessingType.SIMPLE_CHAT, Map.of());
        when(languageProcessor.evaluateSimpleReply(any(), any(), any())).thenReturn(simpleResult);
        when(simpleDecoder.decode(simpleResult)).thenReturn(new SimpleChatStrategyResponse(true, "Hi"));

        String out = aiService.requestHandler("hello", AuthInfo.builder().role("USER").firstName("A").userId(1L).build());
        assertThat(out).isEqualTo("Hi");
    }

    @Test
    void requestHandler_complexPath_executesToolsAndGeneratesFinalOutput() throws Exception {
        when(embeddingService.generateEmbedding(any())).thenReturn(new float[]{1f,2f,3f});
        when(staticInfoRepository.findSimilarStaticInfo(any(), anyInt())).thenReturn(List.of());

        ProcessingResult first = new ProcessingResult("{\"isSimple\":false,\"data\":\"Need tools\"}", OutputFormat.JSON, ProcessingType.SIMPLE_CHAT, Map.of());
        when(languageProcessor.evaluateSimpleReply(any(), any(), any())).thenReturn(first);
        when(simpleDecoder.decode(first)).thenReturn(new SimpleChatStrategyResponse(false, "Need tools"));

        // Workflow step embeddings search
        when(workflowStepRepository.findSimilarSteps(any(), anyInt())).thenReturn(List.of());

        // Tool call identification
        ProcessingResult toolIdentify = new ProcessingResult("{\"tool_calls\":[{" +
                "\"toolName\":\"echo\",\"parameters\":{}}]}", OutputFormat.JSON, ProcessingType.TOOL_CALL_IDENTIFICATION, Map.of());
        when(languageProcessor.findHelperToolCalls(any(), any(), any())).thenReturn(toolIdentify);
        when(toolCallDecoder.decode(any())).thenReturn(List.of(new ToolCall("echo", Map.of())));
        when(toolExecutionService.executeAll(any(), any(), any())).thenReturn(List.of(ToolCallResult.success("res", "echo")));

        // Final output
        ProcessingResult finalProc = new ProcessingResult("{\"isComplete\":true,\"data\":\"DONE\"}", OutputFormat.JSON, ProcessingType.FINAL_OUTPUT_GENERATION, Map.of());
        when(languageProcessor.finalOutputPrepWithData(any(), any(), any())).thenReturn(finalProc);
        when(finalDecoder.decode(finalProc)).thenReturn(new com.voidsquad.chatbot.decoder.FinalOutputStrategyResponse(true, "DONE"));

        String out = aiService.requestHandler("complex", AuthInfo.builder().role("ADMIN").firstName("B").userId(5L).build());
        assertThat(out).isEqualTo("DONE");
    }

    @Test
    void requestHandler_withoutCoreDeps_fallsBackToGeneration() throws Exception {
        // EmbeddingService mocked but we simulate missing other deps by setting them null via reflection (simpler: return null ProcessingResult)
        when(languageProcessor.evaluateSimpleReply(any(), any(), any())).thenReturn(new ProcessingResult("fallback", OutputFormat.TEXT, ProcessingType.SIMPLE_CHAT, Map.of()));
        AIService local = new AIService(embeddingService, null, languageProcessor, null, simpleDecoder, finalDecoder, workflowStepRepository, toolCallDecoder, toolRegistry, toolExecutionService);
        String out = local.requestHandler("x", AuthInfo.builder().role("X").build());
        assertThat(out).isEqualTo("fallback");
    }
}
