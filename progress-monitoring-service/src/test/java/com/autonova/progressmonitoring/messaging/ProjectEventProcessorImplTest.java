package com.autonova.progressmonitoring.messaging;

import com.autonova.progressmonitoring.messaging.mapper.DefaultEventMessageMapper;
import com.autonova.progressmonitoring.messaging.mapper.EventMessageMapper;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.messaging.rabbit.ProjectEventProcessorImpl;
import com.autonova.progressmonitoring.service.ProjectMessageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ProjectEventProcessorImplTest {

    @Mock
    private EventPublisher publisher;

    @Mock
    private ProjectMessageService messageService;

    private ObjectMapper mapper;
    private ProjectEventProcessorImpl processor;

    @BeforeEach
    void setUp() {
        openMocks(this);
        mapper = new ObjectMapper();
        // use the default mapper implementation in tests
        EventMessageMapper messageMapper = new DefaultEventMessageMapper();
        processor = new ProjectEventProcessorImpl(publisher, mapper, messageMapper, messageService);
    }

    @Test
    void process_withProjectId_callsPublishToProject() {
        String json = "{\"projectId\":\"1111-2222-3333\", \"status\":\"updated\"}";
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("project.updated");
        Message msg = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        processor.process(msg);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher).publishToProject(idCaptor.capture(), payloadCaptor.capture());

        assertEquals("1111-2222-3333", idCaptor.getValue());
        assertEquals(json, payloadCaptor.getValue());

        // ensure we persisted the message
        verify(messageService).saveMessage(org.mockito.ArgumentMatchers.eq(java.util.UUID.fromString("1111-2222-3333")), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(json), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void process_withoutProjectId_callsBroadcast() {
        String json = "{\"foo\":\"bar\"}";
        MessageProperties props = new MessageProperties();
        Message msg = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        processor.process(msg);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher).broadcast(payloadCaptor.capture());
        assertEquals(json, payloadCaptor.getValue());
    }

    @Test
    void process_invalidJson_callsBroadcast() {
        String body = "not-a-json";
        MessageProperties props = new MessageProperties();
        Message msg = new Message(body.getBytes(StandardCharsets.UTF_8), props);

        processor.process(msg);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher).broadcast(payloadCaptor.capture());
        assertEquals(body, payloadCaptor.getValue());
    }
}
