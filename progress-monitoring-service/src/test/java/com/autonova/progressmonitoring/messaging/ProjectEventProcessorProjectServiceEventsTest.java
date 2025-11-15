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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ProjectEventProcessorProjectServiceEventsTest {

    @Mock
    private EventPublisher publisher;

    @Mock
    private ProjectMessageService messageService;

    private ProjectEventProcessorImpl processor;

    @BeforeEach
    void setUp() {
        openMocks(this);
        ObjectMapper mapper = new ObjectMapper();
        EventMessageMapper messageMapper = new DefaultEventMessageMapper();
        processor = new ProjectEventProcessorImpl(publisher, mapper, messageMapper, messageService);
    }

    @Test
    void process_quoteApproved_publishesAndPersists() {
        String projectId = UUID.randomUUID().toString();
        String quoteId = UUID.randomUUID().toString();
        String json = "{" +
                "\"projectId\":\"" + projectId + "\"," +
                "\"quoteId\":\"" + quoteId + "\"," +
                "\"occurredAt\":\"2025-01-01T00:00:00Z\"}";
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("quote.approved");
        Message msg = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        processor.process(msg);

        // publish raw
        verify(publisher).publishToProject(eq(projectId), eq(json));
        // publish human-friendly
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher).publishMessageToProject(eq(projectId), messageCaptor.capture());

        // message should mention Quote and approved
        String friendly = messageCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(friendly.toLowerCase()).contains("quote").contains("approved");

        // persisted
        verify(messageService).saveMessage(eq(UUID.fromString(projectId)), any(), any(), eq(json), any());
    }

    @Test
    void process_changeRequestApplied_publishesAndPersists() {
        String projectId = UUID.randomUUID().toString();
        String changeRequestId = UUID.randomUUID().toString();
        String json = "{" +
                "\"projectId\":\"" + projectId + "\"," +
                "\"changeRequestId\":\"" + changeRequestId + "\"," +
                "\"occurredAt\":\"2025-01-01T00:00:00Z\"}";
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("project.change-request.applied");
        Message msg = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        processor.process(msg);

        verify(publisher).publishToProject(eq(projectId), eq(json));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher).publishMessageToProject(eq(projectId), messageCaptor.capture());

        String friendly = messageCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(friendly.toLowerCase()).contains("change request").contains("applied");

        verify(messageService).saveMessage(eq(UUID.fromString(projectId)), any(), any(), eq(json), any());
    }
}
