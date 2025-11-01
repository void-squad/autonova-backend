package com.autonova.progressmonitoring.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SseEmitterRegistryTest {

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
    }

    static class TestSseEmitter extends SseEmitter {
        final AtomicInteger sendCount = new AtomicInteger();
        final boolean shouldThrow;

        TestSseEmitter(long timeout, boolean shouldThrow) {
            super(timeout);
            this.shouldThrow = shouldThrow;
        }

        @Override
        public void send(SseEmitter.SseEventBuilder event) throws IOException {
            if (shouldThrow) throw new IOException("broken");
            sendCount.incrementAndGet();
        }

        // Keep default behavior for other overloads
    }

    @Test
    void publishToProject_sendsEventToRegisteredEmitters() throws Exception {
        TestSseEmitter emitter = new TestSseEmitter(0L, false);
        registry.register("proj-1", emitter);

        registry.publishToProject("proj-1", "{\"x\":1}");

        assertEquals(1, emitter.sendCount.get());
    }

    @Test
    void publishToProject_removesEmitterOnSendException() throws Exception {
        TestSseEmitter broken = new TestSseEmitter(0L, true);
        TestSseEmitter good = new TestSseEmitter(0L, false);

        registry.register("proj-2", broken);
        registry.register("proj-2", good);

        // first publish: broken emitter will throw and be removed; good emitter should receive the event
        registry.publishToProject("proj-2", "{\"x\":2}");

        assertEquals(0, broken.sendCount.get());
        assertEquals(1, good.sendCount.get());

        // second publish: only good emitter remains and should receive another event
        registry.publishToProject("proj-2", "{\"x\":3}");
        assertEquals(0, broken.sendCount.get());
        assertEquals(2, good.sendCount.get());
    }

    @Test
    void broadcastToAll_sendsToAllProjects() throws Exception {
        TestSseEmitter e1 = new TestSseEmitter(0L, false);
        TestSseEmitter e2 = new TestSseEmitter(0L, false);

        registry.register("p1", e1);
        registry.register("p2", e2);

        registry.broadcast("{\"all\":true}");

        assertEquals(1, e1.sendCount.get());
        assertEquals(1, e2.sendCount.get());
    }
}
