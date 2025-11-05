package com.autonova.progressmonitoring.messaging.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitProjectConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitProjectConsumer.class);

    private final ProjectEventProcessor processor;

    public RabbitProjectConsumer(ProjectEventProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "${app.rabbit.queue:progress.project.queue}")
    public void onMessage(Message message) {
        try {
            processor.process(message);
        } catch (Exception ex) {
            log.error("Error handling rabbit message", ex);
        }
    }
}
