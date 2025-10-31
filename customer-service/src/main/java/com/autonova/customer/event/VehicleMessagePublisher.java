package com.autonova.customer.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VehicleMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public VehicleMessagePublisher(RabbitTemplate rabbitTemplate,
                                   @Value("${customer.events.exchange:customer.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVehicleEvent(VehicleEvent event) {
        String routingKey = "vehicle." + event.type().name().toLowerCase();
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
