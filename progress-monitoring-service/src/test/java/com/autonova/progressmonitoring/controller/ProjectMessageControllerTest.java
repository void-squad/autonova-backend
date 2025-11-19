package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.client.ProjectServiceClient.ProjectServiceException;
import com.autonova.progressmonitoring.dto.CreateStatusRequest;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.factory.ProjectMessageFactory;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.service.ProjectClientService;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import com.autonova.progressmonitoring.storage.AttachmentStorage;
import com.autonova.progressmonitoring.storage.StoredAttachment;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMessageControllerTest {

    @Mock
    private ProjectMessageService service;

    @Mock
    private EventPublisher publisher;

    @Mock
    private AttachmentStorage attachmentStorage;

    @Mock
    private ProjectClientService projectClientService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    private ProjectMessageController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectMessageController(service, publisher, attachmentStorage, projectClientService, objectMapper);
    }

    @Test
    void getMyProjectStatuses_withNullUserId_returnsUnauthorized() {
        when(request.getAttribute("userId")).thenReturn(null);

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyProjectStatuses(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyProjectStatuses_withValidUserId_returnsProjectStatuses() throws Exception {
        Long userId = 123L;
        when(request.getAttribute("userId")).thenReturn(userId);

        UUID projectId = UUID.randomUUID();
        Map<String, Object> project = new HashMap<>();
        project.put("projectId", projectId.toString());
        project.put("title", "Test Project");

        List<Map<String, Object>> projects = Collections.singletonList(project);
        when(projectClientService.getProjectsForCustomer(userId)).thenReturn(projects);

        ProjectMessageDto message = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .category("info")
                .message("Test message")
                .occurredAt(OffsetDateTime.now())
                .build();
        when(service.getMessagesForProjectDto(projectId)).thenReturn(Collections.singletonList(message));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyProjectStatuses(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsKey("project");
        assertThat(response.getBody().get(0)).containsKey("lastMessage");
    }

    @Test
    void getMyProjectStatuses_withEmptyProjects_returnsEmptyList() throws Exception {
        Long userId = 123L;
        when(request.getAttribute("userId")).thenReturn(userId);
        when(projectClientService.getProjectsForCustomer(userId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyProjectStatuses(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getMyProjectStatuses_withInvalidUserId_returnsInternalServerError() {
        when(request.getAttribute("userId")).thenReturn("invalid");

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyProjectStatuses(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getMessages_withValidProjectId_returnsMessages() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectMessageDto message = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message("Test message")
                .build();

        List<ProjectMessageDto> messages = Collections.singletonList(message);
        when(service.getMessagesForProjectDto(projectId)).thenReturn(messages);
        doNothing().when(projectClientService).enrichMessagesWithProjectTitle(projectId.toString(), messages);

        ResponseEntity<List<ProjectMessageDto>> response = controller.getMessages(projectId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(projectClientService).enrichMessagesWithProjectTitle(projectId.toString(), messages);
    }

    @Test
    void getMessages_withInvalidProjectId_returnsBadRequest() {
        ResponseEntity<List<ProjectMessageDto>> response = controller.getMessages("invalid-uuid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMessages_withProjectServiceException_returnsInternalServerError() throws Exception {
        UUID projectId = UUID.randomUUID();
        List<ProjectMessageDto> messages = Collections.singletonList(
                ProjectMessageDto.builder().id(UUID.randomUUID()).build()
        );
        when(service.getMessagesForProjectDto(projectId)).thenReturn(messages);
        doThrow(new ProjectServiceException("Service error", 500))
                .when(projectClientService).enrichMessagesWithProjectTitle(projectId.toString(), messages);

        ResponseEntity<List<ProjectMessageDto>> response = controller.getMessages(projectId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getMessagesPage_withValidProjectId_returnsPage() {
        UUID projectId = UUID.randomUUID();
        ProjectMessageDto message = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message("Test message")
                .build();

        Slice<ProjectMessageDto> slice = new SliceImpl<>(Collections.singletonList(message));
        when(service.getMessagesPage(projectId, 0, 20)).thenReturn(slice);

        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesPage(projectId.toString(), 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getMessagesPage_withInvalidProjectId_returnsBadRequest() {
        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesPage("invalid-uuid", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMessagesBefore_withValidProjectId_returnsMessages() {
        UUID projectId = UUID.randomUUID();
        OffsetDateTime before = OffsetDateTime.now();
        ProjectMessageDto message = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message("Test message")
                .build();

        Slice<ProjectMessageDto> slice = new SliceImpl<>(Collections.singletonList(message));
        when(service.getMessagesBefore(projectId, before, 20)).thenReturn(slice);

        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesBefore(projectId.toString(), before, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getMessagesBefore_withInvalidProjectId_returnsBadRequest() {
        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesBefore("invalid-uuid", OffsetDateTime.now(), 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMessagesAfter_withValidProjectId_returnsMessages() {
        UUID projectId = UUID.randomUUID();
        OffsetDateTime after = OffsetDateTime.now();
        ProjectMessageDto message = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message("Test message")
                .build();

        Slice<ProjectMessageDto> slice = new SliceImpl<>(Collections.singletonList(message));
        when(service.getMessagesAfter(projectId, after, 20)).thenReturn(slice);

        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesAfter(projectId.toString(), after, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getMessagesAfter_withInvalidProjectId_returnsBadRequest() {
        ResponseEntity<Slice<ProjectMessageDto>> response = controller.getMessagesAfter("invalid-uuid", OffsetDateTime.now(), 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postStatusMessage_withValidRequest_createsMessage() throws Exception {
        UUID projectId = UUID.randomUUID();
        CreateStatusRequest request = new CreateStatusRequest();
        request.setMessage("Test message");
        request.setCategory("info");

        ProjectMessageDto savedMessage = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message("Test message")
                .category("info")
                .build();

        when(service.saveMessage(any(ProjectMessageDto.class))).thenReturn(savedMessage);
        when(objectMapper.writeValueAsString(savedMessage)).thenReturn("{}");

        ResponseEntity<ProjectMessageDto> response = controller.postStatusMessage(projectId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(savedMessage);
        verify(publisher).publishToProject(eq(projectId.toString()), anyString());
    }

    @Test
    void postStatusMessage_withInvalidProjectId_returnsBadRequest() {
        CreateStatusRequest request = new CreateStatusRequest();
        request.setMessage("Test message");

        ResponseEntity<ProjectMessageDto> response = controller.postStatusMessage("invalid-uuid", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postStatusMessage_withBlankMessage_returnsBadRequest() {
        UUID projectId = UUID.randomUUID();
        CreateStatusRequest request = new CreateStatusRequest();
        request.setMessage("  ");

        ResponseEntity<ProjectMessageDto> response = controller.postStatusMessage(projectId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postStatusMessage_withNullMessage_returnsBadRequest() {
        UUID projectId = UUID.randomUUID();
        CreateStatusRequest request = new CreateStatusRequest();
        request.setMessage(null);

        ResponseEntity<ProjectMessageDto> response = controller.postStatusMessage(projectId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAndCreateMessage_withValidFile_createsMessage() throws Exception {
        UUID projectId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        String message = "Test message";
        String category = "info";

        StoredAttachment attachment = StoredAttachment.builder()
                .url("url")
                .contentType("text/plain")
                .originalFilename("test.txt")
                .size(7L)
                .build();
        when(attachmentStorage.store(file)).thenReturn(attachment);

        ProjectMessageDto savedMessage = ProjectMessageDto.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .message(message)
                .category(category)
                .attachmentUrl(attachment.getUrl())
                .build();

        when(service.saveMessage(any(ProjectMessageDto.class))).thenReturn(savedMessage);
        when(objectMapper.writeValueAsString(savedMessage)).thenReturn("{}");

        ResponseEntity<ProjectMessageDto> response = controller.uploadAndCreateMessage(projectId.toString(), file, message, category);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(savedMessage);
        verify(attachmentStorage).store(file);
        verify(publisher).publishToProject(eq(projectId.toString()), anyString());
    }

    @Test
    void uploadAndCreateMessage_withInvalidProjectId_returnsBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        ResponseEntity<ProjectMessageDto> response = controller.uploadAndCreateMessage("invalid-uuid", file, "message", "info");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAndCreateMessage_withEmptyFile_returnsBadRequest() {
        UUID projectId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);

        ResponseEntity<ProjectMessageDto> response = controller.uploadAndCreateMessage(projectId.toString(), file, "message", "info");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAndCreateMessage_withBlankMessage_returnsBadRequest() {
        UUID projectId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        ResponseEntity<ProjectMessageDto> response = controller.uploadAndCreateMessage(projectId.toString(), file, "  ", "info");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void test_returnsTestString() {
        String result = controller.test();

        assertThat(result).isEqualTo("Test");
    }
}
