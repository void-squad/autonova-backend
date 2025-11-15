package com.autonova.progressmonitoring.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoredAttachment {
    String url;
    String contentType;
    String originalFilename;
    long size;
}

