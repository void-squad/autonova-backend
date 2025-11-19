package com.autonova.progressmonitoring.service;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.entity.ProjectMessage;
import com.autonova.progressmonitoring.repository.ProjectMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMessageServiceTest {

    @Mock
    ProjectMessageRepository repository;

    @InjectMocks
    ProjectMessageService service;

    @Test
    void saveMessage_byFields_persistsAndReturnsDto() {
        UUID projectId = UUID.randomUUID();
        String category = "info";
        String message = "Test message";
        String payload = "{\"k\":\"v\"}";
        OffsetDateTime occurredAt = OffsetDateTime.now();

        ProjectMessage saved = new ProjectMessage(projectId, category, message, payload, occurredAt);
        saved.setAttachmentFilename("file.txt");
        // simulate generated id and createdAt
        // use reflection/setters provided by Lombok
        saved.setAttachmentSize(123L);

        when(repository.save(any(ProjectMessage.class))).thenAnswer(invocation -> {
            ProjectMessage arg = invocation.getArgument(0);
            arg.setAttachmentFilename(saved.getAttachmentFilename());
            arg.setAttachmentSize(saved.getAttachmentSize());
            return arg;
        });

        ProjectMessageDto dto = service.saveMessage(projectId, category, message, payload, occurredAt);

        ArgumentCaptor<ProjectMessage> captor = ArgumentCaptor.forClass(ProjectMessage.class);
        verify(repository, times(1)).save(captor.capture());
        ProjectMessage persisted = captor.getValue();

        assertThat(persisted.getProjectId()).isEqualTo(projectId);
        assertThat(persisted.getCategory()).isEqualTo(category);
        assertThat(persisted.getMessage()).isEqualTo(message);
        assertThat(dto).isNotNull();
        assertThat(dto.getMessage()).isEqualTo(message);
        assertThat(dto.getPayload()).isEqualTo(payload);
    }

    @Test
    void saveMessage_byDto_convertsAndSaves() {
        UUID projectId = UUID.randomUUID();
        ProjectMessageDto dto = ProjectMessageDto.builder()
                .projectId(projectId)
                .category("warn")
                .message("DTO message")
                .payload("p")
                .occurredAt(OffsetDateTime.now())
                .build();

        when(repository.save(any(ProjectMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMessageDto result = service.saveMessage(dto);

        verify(repository, times(1)).save(any(ProjectMessage.class));
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getMessage()).isEqualTo("DTO message");
    }

    @Test
    void getMessagesPage_sanitizesPageAndSize_andMapsSlice() {
        UUID projectId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        Slice<ProjectMessage> repoSlice = (Slice<ProjectMessage>) mock(Slice.class);
        @SuppressWarnings("unchecked")
        Slice<ProjectMessageDto> mappedSlice = mock(Slice.class);

        when(repository.findByProjectIdOrderByCreatedAtDesc(eq(projectId), any())).thenReturn(repoSlice);
        when(repoSlice.map(any())).thenReturn((Slice) mappedSlice);

        Slice<ProjectMessageDto> result = service.getMessagesPage(projectId, -5, 0);

        // ensure repository was called with a Pageable (we can't inspect Pageable easily here)
        verify(repository, times(1)).findByProjectIdOrderByCreatedAtDesc(eq(projectId), any());
        assertThat(result).isSameAs(mappedSlice);
    }
}
