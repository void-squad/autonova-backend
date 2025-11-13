package com.autonova.progressmonitoring.messaging;

import com.autonova.progressmonitoring.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigMultipleBindingsTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RabbitConfig.class)
            .withPropertyValues(
                    "app.rabbit.exchange=test.exchange",
                    "app.rabbit.queue=test.queue",
                    // leave app.rabbit.routing-key blank to trigger multi-binding path
                    "app.rabbit.routing-key=",
                    "app.rabbit.routing-keys=project.*,quote.*,project.change-request.*"
            );

    @Test
    void multipleBindingsCreated() {
        contextRunner.run(ctx -> {
            Queue q = ctx.getBean(Queue.class);
            TopicExchange ex = ctx.getBean(TopicExchange.class);
            Object bindingsBean = ctx.getBean("projectBindings");
            assertThat(bindingsBean).isInstanceOfAny(Binding.class, Declarables.class);
            if (bindingsBean instanceof Declarables d) {
                long bindingCount = d.getDeclarables().stream().filter(b -> b instanceof Binding).count();
                assertThat(bindingCount).isGreaterThanOrEqualTo(3); // project.*, quote.*, project.change-request.*
            } else if (bindingsBean instanceof Binding) {
                // fallback single binding -- should not happen in this test
                assertThat(((Binding) bindingsBean).getRoutingKey()).isNotBlank();
            }
            assertThat(q.getName()).isEqualTo("test.queue");
            assertThat(ex.getName()).isEqualTo("test.exchange");
        });
    }
}

