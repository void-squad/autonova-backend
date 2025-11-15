package com.autonova.progressmonitoring.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalAttachmentStorage implements AttachmentStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalAttachmentStorage.class);
    private final Path root = Path.of("uploads");

    public LocalAttachmentStorage() {
        try { Files.createDirectories(root); } catch (IOException e) {
            log.error("Failed to create uploads dir", e);
            throw new AttachmentStorageException("Failed to initialize storage directory", e);
        }
    }

    @Override
    public StoredAttachment store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty file");
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String sanitized = System.currentTimeMillis() + "_" + original;
        Path target = root.resolve(sanitized);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AttachmentStorageException("Failed to store file", e);
        }
        return StoredAttachment.builder()
                .url("/uploads/" + sanitized)
                .contentType(file.getContentType())
                .originalFilename(original)
                .size(file.getSize())
                .build();
    }
}
