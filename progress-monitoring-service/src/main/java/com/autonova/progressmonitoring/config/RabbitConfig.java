package com.autonova.progressmonitoring.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange:autonova.events}")
    private String exchangeName;

    @Value("${app.rabbit.queue:progress.project.queue}")
    private String queueName;

    // Backward-compatible single routing key (used by existing tests)
    @Value("${app.rabbit.routing-key:}")
    private String singleRoutingKey;

    // New property allowing multiple routing keys (comma-separated)
    @Value("${app.rabbit.routing-keys:project.*,quote.*,project.change-request.*}")
    private String routingKeys;

    @Bean
    public Queue projectQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange projectExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    // If singleRoutingKey is provided, return a single Binding bean for compatibility.
    // Otherwise declare multiple bindings using Declarables.
    @Bean
    public Object projectBindings(Queue projectQueue, TopicExchange projectExchange) {
        if (singleRoutingKey != null && !singleRoutingKey.isBlank()) {
            return BindingBuilder.bind(projectQueue).to(projectExchange).with(singleRoutingKey.trim());
        }

        List<String> keys = parseKeys(routingKeys);
        List<Binding> bindings = keys.stream()
                .map(k -> BindingBuilder.bind(projectQueue).to(projectExchange).with(k))
                .collect(Collectors.toCollection(ArrayList::new));
        return new Declarables(new ArrayList<>(bindings));
    }

    private static List<String> parseKeys(String keysCsv) {
        if (keysCsv == null || keysCsv.isBlank()) return List.of("project.*");
        return Arrays.stream(keysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
