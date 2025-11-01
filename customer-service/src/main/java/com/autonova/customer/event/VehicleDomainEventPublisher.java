package com.autonova.customer.event;

import com.autonova.customer.model.Vehicle;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class VehicleDomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public VehicleDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(VehicleEventType type, Vehicle vehicle) {
        applicationEventPublisher.publishEvent(VehicleEvent.from(type, vehicle));
    }
}
