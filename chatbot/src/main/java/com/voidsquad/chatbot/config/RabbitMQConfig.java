package com.voidsquad.chatbot.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@EnableRabbit
public class RabbitMQConfig {
//
//    public static final String QUEUE = "demo_queue";
//    public static final String EXCHANGE = "demo_exchange";
//    public static final String ROUTING_KEY = "demo_routingKey";
//
////    @Bean
//    public Queue queue() {
//        return new Queue(QUEUE, false);
//    }
//
////    @Bean
//    public TopicExchange exchange() {
//        return new TopicExchange(EXCHANGE);
//    }
//
////    @Bean
//    public Binding binding(Queue queue, TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
//    }
}