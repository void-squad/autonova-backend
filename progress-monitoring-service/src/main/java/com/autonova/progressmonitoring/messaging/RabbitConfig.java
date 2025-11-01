package com.autonova.progressmonitoring.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange:autonova.events}")
    private String exchangeName;

    @Value("${app.rabbit.queue:progress.project.queue}")
    private String queueName;

    @Value("${app.rabbit.routing-key:project.*}")
    private String routingKey;

    @Bean
    public Queue projectQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange projectExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding binding(Queue projectQueue, TopicExchange projectExchange) {
        return BindingBuilder.bind(projectQueue).to(projectExchange).with(routingKey);
    }
}
