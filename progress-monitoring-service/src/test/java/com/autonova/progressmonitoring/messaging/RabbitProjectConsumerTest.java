package com.autonova.progressmonitoring.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.Mockito.mock;

class RabbitProjectConsumerTest {

    private ProjectEventProcessor processor;
    private RabbitProjectConsumer consumer;

    @BeforeEach
    void setUp() {
        openMocks(this);
        processor = mock(ProjectEventProcessor.class);
        consumer = new RabbitProjectConsumer(processor);
    }

    @Test
    void onMessage_delegatesToProcessor() {
        String body = "{\"projectId\":\"p1\"}";
        MessageProperties props = new MessageProperties();
        Message msg = new Message(body.getBytes(StandardCharsets.UTF_8), props);

        consumer.onMessage(msg);

        verify(processor).process(msg);
    }

    @Test
    void onMessage_swallowsProcessorException() {
        String body = "{\"projectId\":\"p2\"}";
        MessageProperties props = new MessageProperties();
        Message msg = new Message(body.getBytes(StandardCharsets.UTF_8), props);

        doThrow(new RuntimeException("boom")).when(processor).process(msg);

        // should not throw
        assertDoesNotThrow(() -> consumer.onMessage(msg));
    }
}
