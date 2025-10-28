package com.autonova.payments_billing_service.messaging;

import com.autonova.payments_billing_service.events.ProjectUpdatedEvent;
import com.autonova.payments_billing_service.service.InvoiceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(ProjectUpdatedListener.class);

    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;

    public ProjectUpdatedListener(ObjectMapper objectMapper, InvoiceService invoiceService) {
        this.objectMapper = objectMapper;
        this.invoiceService = invoiceService;
    }

    @RabbitListener(queues = "${payments.messaging.inbound.project-updated-queue}")
    public void onMessage(String message) {
        try {
            ProjectUpdatedEvent event = objectMapper.readValue(message, ProjectUpdatedEvent.class);
            invoiceService.handleProjectUpdated(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize project.updated payload: {}", message, e);
        }
    }
}
