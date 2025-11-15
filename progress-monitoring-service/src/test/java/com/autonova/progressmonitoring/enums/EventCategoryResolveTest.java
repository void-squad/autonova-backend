package com.autonova.progressmonitoring.enums;

import com.autonova.progressmonitoring.messaging.mapper.DefaultEventMessageMapper;
import com.autonova.progressmonitoring.messaging.mapper.EventMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EventCategoryResolveTest {

    private static Stream<TestCase> data() {
        return Stream.of(
            new TestCase("project.created", EventCategory.CREATED, true, false, false),
            new TestCase("project.updated", EventCategory.UPDATED, true, false, false),
            new TestCase("quote.approved", EventCategory.APPROVED, false, true, false),
            new TestCase("quote.rejected", EventCategory.REJECTED, false, true, false),
            new TestCase("project.change-request.created", EventCategory.CREATED, true, false, true),
            new TestCase("project.change-request.approved", EventCategory.APPROVED, true, false, true),
            new TestCase("project.change-request.rejected", EventCategory.REJECTED, true, false, true),
            new TestCase("project.change-request.applied", EventCategory.APPLIED, true, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Routing keys resolve to correct category and friendly message contains expected tokens")
    void resolvesCategoryAndMapsFriendlyMessage(TestCase tc) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("projectId", java.util.UUID.randomUUID().toString());
        if (tc.isQuote) payload.put("quoteId", java.util.UUID.randomUUID().toString());
        if (tc.isChangeRequest) payload.put("changeRequestId", java.util.UUID.randomUUID().toString());
        payload.put("occurredAt", OffsetDateTime.parse("2025-01-01T00:00:00Z").toString());

        EventCategory resolved = EventCategory.resolve(tc.routingKey, payload);
        assertThat(resolved).isEqualTo(tc.expectedCategory);

        EventMessageMapper messageMapper = new DefaultEventMessageMapper();
        String friendly = messageMapper.mapToMessage(tc.routingKey, payload);

        // Basic assertions on message content
        assertThat(friendly.toLowerCase()).contains(tc.expectedCategory.verb());
        if (tc.isQuote) {
            assertThat(friendly.toLowerCase()).contains("quote");
        } else if (tc.isChangeRequest) {
            assertThat(friendly.toLowerCase()).contains("change request");
        } else {
            assertThat(friendly.toLowerCase()).contains("project");
        }
    }

    private record TestCase(String routingKey, EventCategory expectedCategory, boolean isProject, boolean isQuote, boolean isChangeRequest) {}
}

