package com.autonova.progressmonitoring.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseControllerTest {

    private SseEmitterRegistry registry;
    private SseController controller;

    @BeforeEach
    void setUp() {
        registry = mock(SseEmitterRegistry.class);
        controller = new SseController(registry);
    }

    @Test
    void subscribeToProject_invalidProjectId_throws() {
        assertThrows(IllegalArgumentException.class, () -> controller.subscribeToProject("not-a-uuid"));
    }

    @Test
    void subscribeToProject_registersEmitterAndReturnsIt() throws Exception {
        String projectId = UUID.randomUUID().toString();

        // capture the emitter passed to registry and return it
        ArgumentCaptor<SseEmitter> captor = ArgumentCaptor.forClass(SseEmitter.class);
        when(registry.register(org.mockito.ArgumentMatchers.eq(projectId), captor.capture()))
                .thenAnswer(inv -> inv.getArgument(1));

        SseEmitter emitter = controller.subscribeToProject(projectId);

        verify(registry).register(org.mockito.ArgumentMatchers.eq(projectId), org.mockito.ArgumentMatchers.any(SseEmitter.class));
        assertNotNull(emitter);
    }
}
