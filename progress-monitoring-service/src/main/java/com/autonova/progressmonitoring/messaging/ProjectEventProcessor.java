package com.autonova.progressmonitoring.messaging;

import org.springframework.amqp.core.Message;

public interface ProjectEventProcessor {
    void process(org.springframework.amqp.core.Message message);
}
