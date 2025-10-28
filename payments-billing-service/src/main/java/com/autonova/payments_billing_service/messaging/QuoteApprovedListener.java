package com.autonova.payments_billing_service.messaging;

import com.autonova.payments_billing_service.events.QuoteApprovedEvent;
import com.autonova.payments_billing_service.service.InvoiceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QuoteApprovedListener {

    private static final Logger log = LoggerFactory.getLogger(QuoteApprovedListener.class);

    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;

    public QuoteApprovedListener(ObjectMapper objectMapper, InvoiceService invoiceService) {
        this.objectMapper = objectMapper;
        this.invoiceService = invoiceService;
    }

    @RabbitListener(queues = "${payments.messaging.inbound.quote-approved-queue}")
    public void onMessage(String message) {
        try {
            QuoteApprovedEvent event = objectMapper.readValue(message, QuoteApprovedEvent.class);
            invoiceService.handleQuoteApproved(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize quote.approved payload: {}", message, e);
        }
    }
}
