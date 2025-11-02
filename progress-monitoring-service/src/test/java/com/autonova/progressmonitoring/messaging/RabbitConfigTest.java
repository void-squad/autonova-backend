package com.autonova.progressmonitoring.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RabbitConfig.class)
            .withPropertyValues(
                    "app.rabbit.exchange=test.exchange",
                    "app.rabbit.queue=test.queue",
                    "app.rabbit.routing-key=project.#"
            );

    @Test
    void rabbitBeansCreated_withDefaults() {
        contextRunner.run(ctx -> {
            Queue q = ctx.getBean(Queue.class);
            TopicExchange ex = ctx.getBean(TopicExchange.class);
            Binding binding = ctx.getBean(Binding.class);

            assertThat(q.getName()).isEqualTo("test.queue");
            assertThat(ex.getName()).isEqualTo("test.exchange");
            assertThat(binding.getExchange()).isEqualTo("test.exchange");
        });
    }
}
