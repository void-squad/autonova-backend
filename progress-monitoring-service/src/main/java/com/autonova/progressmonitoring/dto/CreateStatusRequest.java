package com.autonova.progressmonitoring.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateStatusRequest {
    private String category;
    private String message;
    private String payload;
    private OffsetDateTime occurredAt;
    // optional attachment reference (after upload)
    private String attachmentUrl;
    private String attachmentContentType;
    private String attachmentFilename;
    private Long attachmentSize;
}
