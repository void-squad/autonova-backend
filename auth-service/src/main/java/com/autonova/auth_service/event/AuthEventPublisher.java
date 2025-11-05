package com.autonova.auth_service.event;

import com.autonova.auth_service.user.model.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange authEventsExchange;
    private final String loginRoutingKey;

    public AuthEventPublisher(RabbitTemplate rabbitTemplate,
                              TopicExchange authEventsExchange,
                              @Value("${auth.events.login.routing-key:user.logged-in}") String loginRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.authEventsExchange = authEventsExchange;
        this.loginRoutingKey = loginRoutingKey;
    }

    public void publishUserLoggedIn(User user) {
        if (user == null) {
            logger.warn("Attempted to publish login event for null user");
            return;
        }

        AuthUserLoggedInEvent event = buildEvent(user);
        try {
            rabbitTemplate.convertAndSend(authEventsExchange.getName(), loginRoutingKey, event);
            logger.debug("Published auth login event for user {}", user.getEmail());
        } catch (Exception ex) {
            logger.error("Failed to publish auth login event for user {}", user.getEmail(), ex);
            throw ex;
        }
    }

    private AuthUserLoggedInEvent buildEvent(User user) {
        return new AuthUserLoggedInEvent(
                UUID.randomUUID(),
                toStableUuid(user.getId()),
                user.getEmail(),
                resolveFirstName(user.getUserName()),
                resolveLastName(user.getUserName()),
                user.getContactOne(),
                Set.of(user.getRole().name()),
                Instant.now());
    }

    private UUID toStableUuid(Long userId) {
        if (userId == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(("auth-user-" + userId).getBytes(StandardCharsets.UTF_8));
    }

    private String resolveFirstName(String userName) {
        if (userName == null || userName.isBlank()) {
            return null;
        }
        String trimmed = userName.trim();
        int delimiter = trimmed.indexOf(' ');
        return delimiter == -1 ? trimmed : trimmed.substring(0, delimiter);
    }

    private String resolveLastName(String userName) {
        if (userName == null || userName.isBlank()) {
            return null;
        }
        String trimmed = userName.trim();
        int delimiter = trimmed.indexOf(' ');
        return delimiter == -1 ? null : trimmed.substring(delimiter + 1).trim();
    }
}
