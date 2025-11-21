package com.voidsquad.chatbot.service;

import com.voidsquad.chatbot.entities.StaticInfo;
import com.voidsquad.chatbot.repository.StaticInfoRepository;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import com.voidsquad.chatbot.service.language.LanguageProcessor;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    EmbeddingService embeddingService;

    @Mock
    StaticInfoRepository staticInfoRepository;

    @Mock
    LanguageProcessor languageProcessor;

    @InjectMocks
    AIService aiService;

    @Test
    void generation_returnsModelOutput_whenProcessorReturnsResult() {
        ProcessingResult res = new ProcessingResult("hello-world", OutputFormat.TEXT, ProcessingType.SIMPLE_CHAT, null);
        when(languageProcessor.evaluateSimpleReply(any(), any(), any())).thenReturn(res);

        String result = aiService.generation("hi");

        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void readStaticInfoFromCSV_parsesValidRows() throws IOException, Exception {
        String csv = "topic,description\nFeature A,Does A\nFeature B,Does B\n";
        MockMultipartFile file = new MockMultipartFile("file", "static.csv", "text/csv", csv.getBytes());

        List<StaticInfo> out = aiService.ReadStaticInfoFromCSV(file);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getTopic()).isEqualTo("Feature A");
        assertThat(out.get(1).getDescription()).isEqualTo("Does B");
    }

    @Test
    void getAllStaticInfoByEmbeddings_formatsResults() {
        when(embeddingService.generateEmbedding(any())).thenReturn(new float[]{1f, 2f, 3f});
        when(staticInfoRepository.findSimilarStaticInfo(any(float[].class), org.mockito.ArgumentMatchers.anyInt())).thenReturn(
                List.of(StaticInfo.builder().topic("T1").description("D1").build())
        );

        List<String> list = aiService.getAllStaticInfoByEmbeddings("keyword");

        assertThat(list).hasSize(1);
        assertThat(list.get(0)).contains("Topic T1").contains("Description: D1");
    }
}
