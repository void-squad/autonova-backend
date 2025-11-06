package com.autonova.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange}")
    private String exchangeName;

    @Value("${app.rabbit.queue}")
    private String queueName;

    @Value("${app.rabbit.routingKey}")
    private String routingKey;

    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue appQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(Queue appQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(appQueue).to(appExchange).with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}

